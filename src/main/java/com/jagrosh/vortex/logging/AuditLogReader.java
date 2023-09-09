package com.jagrosh.vortex.logging;

import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.hibernate.api.ModlogManager;
import com.jagrosh.vortex.hibernate.entities.GuildData;
import com.jagrosh.vortex.utils.*;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.audit.AuditLogChange;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import net.dv8tion.jda.api.audit.AuditLogKey;
import net.dv8tion.jda.api.entities.Guild;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Slf4j
public class AuditLogReader {
    // TODO: Make this reset after its fine after a while
    private volatile int amountOfTimesThisCrashed = 0; // Amount of times start() has crashed, just in case
    private final Vortex vortex;
    private final GuildResourceProvider<GuildAuditLogReader> guildReaderProvider;
    private final BlockingQueue<AuditLogEntry> logsToSync;

    public AuditLogReader(Vortex vortex) {
        this.vortex = vortex;
        this.logsToSync = new LinkedBlockingQueue<>();
        this.guildReaderProvider = new @DoNotUseForVerifiedBots GuildResourceProvider<>(id -> new GuildAuditLogReader(vortex, logsToSync, id, this::handle));
    }

    /**
     * Indicates that the bot should bulk request logs for a specific guild using an {@link net.dv8tion.jda.api.requests.restaction.pagination.AuditLogPaginationAction AuditLogPaginationAction}.
     * This should only be called once after the initial {@link net.dv8tion.jda.api.events.session.ReadyEvent}
     * @param g The guild
     */
    public void bulkRead(Guild g) {
        guildReaderProvider.get(g).bulkRead(g);
    }

    /**
     * Requests for an {@link AuditLogEntry} to be handled by the bot.
     * @param entry The entry to parse
     */
    public void parseEntry(AuditLogEntry entry) {
        guildReaderProvider.get(entry.getGuild()).parseEntry(entry);
    }

    // TODO: Consider moving to own class
    /**
     * The main handling method for audit logs. To ensure proper concurrency, this method should only be called from a {@link GuildAuditLogReader} instance.
     * For forwarding a specific log to the audit log parsing system, use {@link #parseEntry(AuditLogEntry)} instead.
     * @param entry The entry to handle
     */
    private void handle(AuditLogEntry entry) {
        if (entry.getUserIdLong() == vortex.getJda().getSelfUser().getIdLong()) {
            return;
        }

        Guild g = entry.getGuild();
        GuildData guildData = vortex.getHibernate().guild_data.getGuildData(g.getIdLong());
        String reason = entry.getReason() == null ? "" : entry.getReason();
        long userId = entry.getUserIdLong();
        long targetId = entry.getTargetIdLong();
        Instant timeCreated = entry.getTimeCreated().toInstant();

        // TODO: Fix thing where past logs may interfere with present logs
        switch (entry.getType()) {
            case KICK -> vortex.getHibernate().modlogs.logKick(g.getIdLong(), targetId, userId, timeCreated, reason);
            case BAN -> vortex.getHibernate().modlogs.logBan(g.getIdLong(), targetId, userId, timeCreated, ModlogManager.INDEFINITE_TIME, reason);
            case UNBAN -> vortex.getHibernate().modlogs.logUnban(g.getIdLong(), targetId, userId, timeCreated);
            case MEMBER_ROLE_UPDATE -> {
                List<Long> addedRoles = extractPartialIds(entry, AuditLogKey.MEMBER_ROLES_ADD);
                List<Long> removedRoles = extractPartialIds(entry, AuditLogKey.MEMBER_ROLES_REMOVE);

                for (int i = 0; i < addedRoles.size(); i++) {
                    long id = addedRoles.get(i);
                    if (id == guildData.getMutedRoleId()) {
                        vortex.getHibernate().modlogs.logMute(g.getIdLong(), targetId, userId, timeCreated, ModlogManager.INDEFINITE_TIME, reason);
                        addedRoles.remove(i--);
                    } else if (id == guildData.getGravelRoleId()) {
                        vortex.getHibernate().modlogs.logGravel(g.getIdLong(), targetId, userId, timeCreated, ModlogManager.INDEFINITE_TIME, reason);
                        addedRoles.remove(i--);
                    }
                }

                for (int i = 0; i < removedRoles.size(); i++) {
                    long id = removedRoles.get(i);
                    if (id == guildData.getMutedRoleId()) {
                        vortex.getHibernate().modlogs.logUnmute(g.getIdLong(), targetId, userId, timeCreated);
                        removedRoles.remove(i--);
                    } else if (id == guildData.getGravelRoleId()) {
                        vortex.getHibernate().modlogs.logUngravel(g.getIdLong(), targetId, userId, timeCreated);
                        removedRoles.remove(i--);
                    }
                }

                if (!addedRoles.isEmpty() || !removedRoles.isEmpty()) {
                    vortex.getBasicLogger().logMemberRoleUpdate(g, targetId, userId, addedRoles, removedRoles, entry.getTimeCreated());
                }
            }
        }
    }

    public static List<Long> extractPartialIds(AuditLogEntry entry, AuditLogKey key) {
        AuditLogChange change = entry.getChangeByKey(key);
        if (change == null) {
            return Collections.emptyList();
        }

        List<Map<String, ?>> partialEntityList = change.getNewValue();
        if (partialEntityList == null || partialEntityList.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> roleIds = new ArrayList<>(partialEntityList.size());
        for (Map<String, ?> partialEntity : partialEntityList) {
            long roleId = switch(partialEntity.get("id")) {
                case Long id -> id;
                case String id -> {
                    try {
                        yield Long.parseLong(id);
                    } catch (NumberFormatException e) {
                        log.error("Could not resolve id", e);
                        yield -1;
                    }
                }
                case null -> {
                    log.error("Could not resolve id");
                    yield -1;
                }
                case Object o -> {
                    log.error("Could not resolve id from Object: " + o.getClass().getName() + " with toString " + o);
                    yield -1;
                }
            };

            if (roleId != -1) {
                roleIds.add(roleId);
            }
        }

        return roleIds;
    }

    /**
     * Starts syncing the most recent audit log entry from each guild to the database.
     */
    public void start() {
        // TODO: Seperate on virtual threads maybe
        vortex.getJda().getGuildCache().parallelStream().forEach(this::bulkRead);

        // TODO: Is this neccessary if a higher level cache is implemented?
        try {
            final int ITERATION_MAX = 1 << 5; // The max amount of entries that can be taken from the queue in a single iteration
            TemporaryBuffer<AuditLogEntry> entryBuffer = new TemporaryBuffer<>(ITERATION_MAX);
            TemporaryBuffer<AuditLogEntry> maxEntryBuffer = new TemporaryBuffer<>(ITERATION_MAX);

            while (true) {
                entryBuffer.add(logsToSync.take());
                logsToSync.drainTo(entryBuffer, ITERATION_MAX - 1);
                outer: for (int i = entryBuffer.size() - 1; i >= 0; i--) {
                    AuditLogEntry entry = entryBuffer.get(i);
                    for (int j = 0; j < maxEntryBuffer.size(); j++) {
                        AuditLogEntry maxEntry = maxEntryBuffer.get(j);
                        if (entry.getGuild().equals(maxEntry.getGuild())) {
                            maxEntryBuffer.set(j, entry);
                            continue outer;
                        }
                    }

                    maxEntryBuffer.add(entry);
                }

                for (AuditLogEntry maxEntry : maxEntryBuffer) {
                    vortex.getHibernate().guild_data.setLastParsed(maxEntry.getGuild().getIdLong(), maxEntry.getIdLong());
                }

                entryBuffer.clear();
                maxEntryBuffer.clear();
            }
        } catch (Exception e) {
            if (amountOfTimesThisCrashed++ < 32) {
                log.error("Exception while syncing audit logs to database", e);
                vortex.getThreadpool().schedule(this::start, 1L << amountOfTimesThisCrashed, TimeUnit.SECONDS);

            } else {
                log.error("Error while syncing audit logs to database. Will not reattempt.", e);
            }
        }
    }
}