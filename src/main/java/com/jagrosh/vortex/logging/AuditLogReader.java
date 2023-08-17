package com.jagrosh.vortex.logging;

import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.utils.DoNotUseForVerifiedBots;
import com.jagrosh.vortex.utils.GuildResourceProvider;
import com.jagrosh.vortex.utils.GuildSettingsCache;
import com.jagrosh.vortex.utils.TemporaryBuffer;
import lombok.extern.java.Log;
import net.dv8tion.jda.api.audit.AuditLogChange;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import net.dv8tion.jda.api.audit.AuditLogKey;
import net.dv8tion.jda.api.entities.Guild;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

@Log
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
        GuildSettingsCache gCache = new GuildSettingsCache(vortex, g);
        String reason = entry.getReason() == null ? "" : entry.getReason();
        long userId = entry.getUserIdLong();
        long targetId = entry.getTargetIdLong();

        switch (entry.getType()) {
            case KICK -> vortex.getDatabase().kicks.logCase(vortex, g, targetId, userId, reason);
            case BAN -> vortex.getDatabase().tempbans.setBan(vortex, g, targetId, userId, Instant.MAX, reason);
            case UNBAN -> vortex.getDatabase().tempbans.clearBan(vortex, g, targetId, userId);
            case MEMBER_ROLE_UPDATE -> {
                for (long id : getPartialRoles(entry, AuditLogKey.MEMBER_ROLES_ADD)) {
                    if (id == gCache.getMutedRoleId()) {
                        vortex.getDatabase().tempmutes.overrideMute(g, targetId, userId, Instant.MAX, reason);
                    } else if (id == gCache.getGraveledRoleId()) {
                        vortex.getDatabase().gravels.overrideGravel(g, targetId, userId, Instant.MAX, reason);
                    }
                }

                for (long id : getPartialRoles(entry, AuditLogKey.MEMBER_ROLES_REMOVE)) {
                    if (id == gCache.getMutedRoleId()) {
                        vortex.getDatabase().tempmutes.removeMute(g, targetId, userId);
                    } else if (id == gCache.getGraveledRoleId()) {
                        vortex.getDatabase().gravels.removeGravel(g, targetId, userId);
                    }
                }
            }
        }

        vortex.getBasicLogger().logAuditLogEntry(entry);
    }

    public static List<Long> getPartialRoles(AuditLogEntry entry, AuditLogKey key) {
        AuditLogChange change = entry.getChangeByKey(key);
        if (change == null) {
            return Collections.emptyList();
        }

        List<Map<String, ?>> partialRoles = change.getNewValue();
        if (partialRoles == null || partialRoles.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> roleIds = new ArrayList<>(partialRoles.size());
        for (Map<String, ?> partialRole : partialRoles) {
            long roleId = switch(partialRole.get("id")) {
                case Long id -> id;
                case String id -> {
                    try {
                        yield Long.parseLong(id);
                    } catch (NumberFormatException e) {
                        log.log(Level.SEVERE, "Could not resolve role id", e);
                        yield -1;
                    }
                }
                case null -> {
                    log.log(Level.SEVERE, "Could not resolve role id");
                    yield -1;
                }
                case Object o -> {
                    log.log(Level.SEVERE, "Could not resolve role id from Object: " + o.getClass().getName() + " with toString " + o);
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
                    vortex.getDatabase().auditcache.setLastParsed(maxEntry.getGuild().getIdLong(), maxEntry.getIdLong());
                }

                entryBuffer.clear();
                maxEntryBuffer.clear();
            }
        } catch (Exception e) {
            if (amountOfTimesThisCrashed++ < 32) {
                log.log(Level.SEVERE, "Exception while syncing audit logs to database", e);
                vortex.getThreadpool().schedule(this::start, 1L << amountOfTimesThisCrashed, TimeUnit.SECONDS);

            } else {
                log.log(Level.SEVERE, "Error while syncing audit logs to database. Will not reattempt.", e);
            }
        }
    }
}