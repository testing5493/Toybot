package com.jagrosh.vortex.hibernate.api;

import com.jagrosh.vortex.hibernate.entities.*;
import com.jagrosh.vortex.utils.DoNotUseForVerifiedBots;
import com.jagrosh.vortex.utils.GuildResourceProvider;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Tuple;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import org.hibernate.Session;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A collection of {@link Database} methods that are in charge of dealing with modlogs
 */
@Slf4j
public class ModlogManager {
    /**
     * Used to indicate that a user should not be pardoned from a punishment by a bot automatically.<br>
     * Set to the max supported second of an {@link Instant}
     */
    public static final Instant INDEFINITE_TIME = Instant.MAX.minusNanos(Instant.MAX.getNano());
    public static final long UNKNOWN_MOD_ID = 123456789L;
    public static final long NOT_YET_PARDONED_MOD_ID = 0L;
    public static final long AUTOMOD_ID = 9999999999L;


    @DoNotUseForVerifiedBots
    private GuildResourceProvider<AtomicInteger> idProvider;
    private final Database database;
    private final GuildResourceProvider<ReentrantLock> guildLock;

    ModlogManager(Database database) {
        this.database = database;
        this.guildLock = new GuildResourceProvider<>(() -> new ReentrantLock());
    }

    @DoNotUseForVerifiedBots
    void init() {
        HashMap<Long, AtomicInteger> maxCasesMap = new HashMap<>();
        List<Tuple> tupleList = database.doTransaction(session -> {
            return session.createQuery("select m.guildId, max(m.caseId) from ModLog m group by m.guildId", Tuple.class)
                    .getResultList();
        });
        tupleList.forEach(tuple -> maxCasesMap.put(tuple.get(0, Long.class), new AtomicInteger(tuple.get(1, Integer.class))));
        idProvider = new GuildResourceProvider<>(maxCasesMap, guildId -> new AtomicInteger());
        log.info("Loaded maximum modlog cases for " + tupleList.size() + " guilds");
        new AtomicInteger().incrementAndGet();
    }

    /**
     * Gets a modlog case from the database
     *
     * @param guildId The guild ID
     * @param caseId The case ID
     * @return A Modlog object, or {@code null} if no log was found
     * @throws PersistenceException If something went wrong while retrieving the modlog
     */
    public ModLog getCase(long guildId, int caseId) throws PersistenceException {
        return database.doTransaction(session -> {
            return getModLog0(session, guildId, caseId);
        });
    }

    /**
     * Gets the modlogs for a specific user from the database
     *
     * @param guildId The guild ID
     * @param userId The user Id
     * @return A list of modlogs for a user on a guild
     * @throws PersistenceException If something went wrong while retrieving the modlogs
     */
    public List<ModLog> getCases(long guildId, long userId) throws PersistenceException {
        return database.doTransaction(session -> { // TODO: Improve ordering
            return session.createQuery("select m from ModLog m where m.guildId=:guildId and m.userId=:userId order by m.caseId desc", ModLog.class)
                    .setParameter("guildId", guildId)
                    .setParameter("userId", userId)
                    .getResultList();
        });
    }


    /**
     * Deletes a modlog case from the database.
     *
     * @param guildId The ID of the guild
     * @param caseId The case ID to delete
     * @return A {@link ModLog} object representing the deleted case, or {@code null} if the case was not found
     * @throws PersistenceException If something went wrong while deleting the case
     */
    public ModLog deleteCase(long guildId, int caseId) throws PersistenceException {
        return database.doTransaction(session -> {
            ModLog modLog = getModLog0(session, guildId, caseId);
            if (modLog == null) {
                return null;
            }

            session.remove(modLog);
            return modLog;
        });
    }

    /**
     * Edits a reason for a modlog case
     *
     * @param guildId The ID of the guild
     * @param caseId The ID of the case to edit
     * @param reason The new reason
     * @return The old reason if successfully edited, or null if the case was not found
     * @throws PersistenceException If something went wrong while editing the reason
     */
    public String editReason(long guildId, int caseId, @Nullable String reason) throws PersistenceException {
        ModLog.Id modLogId = new ModLog.Id(guildId, caseId);
        return database.doTransaction(session -> {
            ModLog modLog = session.get(ModLog.class, modLogId);
            if (modLog == null) {
                return null;
            }

            String oldReason = modLog.getReason();
            modLog.setReason(reason == null ? "" : reason);
            session.merge(modLog);
            return oldReason;
        });
    }

    /**
     * Logs a warning to the database
     *
     * @param guildId The ID of the guild
     * @param userId The ID of the user
     * @param userName The username of the user
     * @param punishingModId The ID of the punishing moderator
     * @param punishingModName The username of the punsihing moderator
     * @param punishingTime The time of the warning
     * @param reason The reason for the warning, or {@code null} if no reason was specified
     * @return The caseId
     * @throws PersistenceException if something went wrong while logging the warning
     */
    public int logWarning(long guildId, long userId, String userName, long punishingModId, String punishingModName, Instant punishingTime, String reason) throws PersistenceException {
        WarnLog warnLog = new WarnLog();
        populate(warnLog, guildId, userId, userName, punishingModId, punishingModName, punishingTime, reason);
        return logPunish(warnLog);
    }

    /**
     * Logs a kick to the database
     *
     * @param guildId The ID of the guild
     * @param userId The ID of the user
     * @param userName The username of the user
     * @param punishingModId The ID of the punishing moderator
     * @param punishingModName The username of the punsihing moderator
     * @param punishingTime The time of the kick
     * @param reason The reason for the kick, or {@code null} if no reason was specified
     * @throws PersistenceException if something went wrong while logging the kick
     */
    public int logKick(long guildId, long userId, String userName, long punishingModId, String punishingModName, Instant punishingTime, String reason) throws PersistenceException {
        KickLog kickLog = new KickLog();
        populate(kickLog, guildId, userId, userName, punishingModId, punishingModName, punishingTime, reason);
        return logPunish(kickLog);
    }

    /**
     * Logs a softban to the database
     *
     * @param guildId The ID of the guild
     * @param userId The ID of the user
     * @param userName The username of the user
     * @param punishingModId The ID of the punishing moderator
     * @param punishingModName The username of the punsihing moderator
     * @param punishingTime The time of the softban
     * @param reason The reason for the softban, or {@code null} if no reason was specified
     * @throws PersistenceException if something went wrong while logging the softban
     */
    public int logSoftban(long guildId, long userId, String userName, long punishingModId, String punishingModName, Instant punishingTime, String reason) throws PersistenceException {
        SoftbanLog softbanLog = new SoftbanLog();
        populate(softbanLog, guildId, userId, userName, punishingModId, punishingModName, punishingTime, reason);
        return logPunish(softbanLog);
    }

    /**
     * Logs a ban to the database
     *
     * @param guildId The ID of the guild
     * @param userId The ID of the user
     * @param userName The username of the user
     * @param punishingModId The ID of the punishing moderator
     * @param punishingModName The username of the punsihing moderator
     * @param punishingTime The time of the punishment in seconds after the unix epoch
     * @param pardoningTime The time the user will be auto-pardoned in seconds after the unix epoch, {@link #INDEFINITE_TIME ModlogManager.INDEFINITE_TIME}
     * @param reason The reason for the ban, or {@code null} if no reason was specified
     * @throws PersistenceException if something went wrong while logging the ban
     */
    public int logBan(long guildId, long userId, String userName, long punishingModId, String punishingModName, Instant punishingTime, Instant pardoningTime, String reason) throws PersistenceException {
        BanLog banLog = new BanLog();
        populate(banLog, guildId, userId, userName, punishingModId, punishingModName, punishingTime, pardoningTime, reason);
        return logPunish(banLog);
    }

    /**
     * Logs a gravel to the database
     *
     * @param guildId The ID of the guild
     * @param userId The ID of the user
     * @param userName The username of the user
     * @param punishingModId The ID of the punishing moderator
     * @param punishingModName The username of the punsihing moderator
     * @param punishingTime The time of the punishment in seconds after the unix epoch
     * @param pardoningTime The time the user will be auto-pardoned in seconds after the unix epoch, {@link #INDEFINITE_TIME ModlogManager.INDEFINITE_TIME}
     * @param reason The reason for the gravel, or {@code null} if no reason was specified
     * @throws PersistenceException if something went wrong while logging the gravel
     */
    public int logGravel(long guildId, long userId, String userName, long punishingModId, String punishingModName, Instant punishingTime, Instant pardoningTime, String reason) throws PersistenceException {
        GravelLog gravelLog = new GravelLog();
        populate(gravelLog, guildId, userId, userName, punishingModId, punishingModName, punishingTime, pardoningTime, reason);
        return logPunish(gravelLog);
    }

    /**
     * Logs a mute to the database
     *
     * @param guildId The ID of the guild
     * @param userId The ID of the user
     * @param userName The username of the user
     * @param punishingModId The ID of the punishing moderator
     * @param punishingModName The username of the punsihing moderator
     * @param punishingTime The time of the punishment in seconds after the unix epoch
     * @param pardoningTime The time the user will be auto-pardoned in seconds after the unix epoch, {@link #INDEFINITE_TIME ModlogManager.INDEFINITE_TIME}
     * @param reason The reason for the gravel, or {@code null} if no reason was specified
     * @throws PersistenceException if something went wrong while logging the mute
     */
    public int logMute(long guildId, long userId, String userName, long punishingModId, String punishingModName, Instant punishingTime, Instant pardoningTime, String reason) throws PersistenceException {
        MuteLog muteLog = new MuteLog();
        populate(muteLog, guildId, userId, userName, punishingModId, punishingModName, punishingTime, pardoningTime, reason);
        return logPunish(muteLog);
    }

    /**
     * Logs an unban to the database
     *
     * @param guildId The ID of the guild
     * @param userId The ID of the user
     * @param pardoningModId The ID of the unbanning mod
     * @param pardoningModName The username of the unbanning mod
     * @param pardoningTime The time the user was unbanned
     * @throws PersistenceException if something went wrong while logging the unban
     */
    public BanLog logUnban(long guildId, long userId, long pardoningModId, String pardoningModName, Instant pardoningTime) {
        return logPardon(BanLog.class, guildId, userId, pardoningModId, pardoningModName, pardoningTime);
    }

    /**
     * Logs an unban to the database
     *
     * @param banLog A ban log object to mark as unbanned, with the guild, user, pardoning mod, and pardoning time
     * @return A complete ban log object
     * @implNote This is purely a wrapper for {@link ModlogManager#logUnban(long, long, long, String, Instant)}
     * @throws PersistenceException if something went wrong while logging the unban
     */
    public BanLog logUnban(BanLog banLog) {
        return logUnban(banLog.getGuildId(), banLog.getUserId(), banLog.getPardoningModId(), banLog.getPardoningModName(), banLog.getPardoningTime());
    }

    /**
     * Logs an ungravel to the database
     *
     * @param gravelLog A gravel log object to mark as ungraveled, with the guild, user, pardoning mod, and pardoning time
     * @return A complete gravel log object
     * @implNote This is purely a wrapper for {@link ModlogManager#logUngravel(long, long, long, String, Instant)}
     * @throws PersistenceException if something went wrong while logging the ungravel
     */
    public GravelLog logUngravel(GravelLog gravelLog) {
        return logUngravel(gravelLog.getGuildId(), gravelLog.getUserId(), gravelLog.getPardoningModId(), gravelLog.getPardoningModName(), gravelLog.getPardoningTime());
    }

    /**
     * Logs an unmute to the database
     *
     * @param muteLog A mute log object to mark as unmuted, with the guild, user, pardoning mod, and pardoning time
     * @return A complete mute log object
     * @implNote This is purely a wrapper for {@link ModlogManager#logUnmute(long, long, long, String, Instant)}
     * @throws PersistenceException if something went wrong while logging the unmute
     */
    public MuteLog logUnmute(MuteLog muteLog) {
        return logUnmute(muteLog.getGuildId(), muteLog.getUserId(), muteLog.getPardoningModId(), muteLog.getPardoningModName(), muteLog.getPardoningTime());
    }

    /**
     * Logs an unmute to the database
     *
     * @param guildId The ID of the guild
     * @param userId The ID of the user
     * @param pardoningModId The ID of the unmuting mod
     * @param pardoningModName The username of the unmuting mod
     * @param pardoningTime The time the user was unmuted
     * @throws PersistenceException if something went wrong while logging the unmute
     */
    public MuteLog logUnmute(long guildId, long userId, long pardoningModId, String pardoningModName, Instant pardoningTime) {
        return logPardon(MuteLog.class, guildId, userId, pardoningModId, pardoningModName, pardoningTime);
    }

    /**
     * Logs an ungravel to the database
     *
     * @param guildId The ID of the guild
     * @param userId The ID of the user
     * @param pardoningModId The ID of the ungraveling mod
     * @param pardoningModName The username of the ungraveling mod
     * @param pardoningTime The time the user was ungraveled
     * @throws PersistenceException if something went wrong while logging the ungravel
     */
    public GravelLog logUngravel(long guildId, long userId, long pardoningModId, String pardoningModName, Instant pardoningTime) {
        return logPardon(GravelLog.class, guildId, userId, pardoningModId, pardoningModName, pardoningTime);
    }

    /**
     * Gets a list of punishments that a user is currently serving in a guild
     *
     * @param guildId The ID of the guild
     * @param userId The ID of the user
     * @return A possibly empty list of {@link TimedLog} that the user is currently serving
     * @throws PersistenceException if something went wrong while retrieving the punishments a user is currently serving
     */
    public List<TimedLog> getCurrentPunishments(long guildId, long userId) throws PersistenceException {
        return database.doTransaction(session -> {
            return session.createQuery("select t from TimedLog t where t.guildId = :guildId and t.userId = :userId and pardoningModId = 0", TimedLog.class)
                    .setParameter("guildId", guildId)
                    .setParameter("userId", userId)
                    .getResultList();
        });
    }

    /**
     * Checks for any logs that need to be auto pardoned
     */
    // TODO: Optimise (?)
    public List<TimedLog> checkAutoPardons() {
        return database.doTransaction(session -> {
            return session.createQuery("select t from TimedLog t where t.pardoningModId = 0 and t.pardoningTime < :now", TimedLog.class)
                    .setParameter("now", Instant.now())
                    .getResultList();
        });
    }

    private void populate(ModLog modLog, long guildId, long userId, String userName, long punishingModId, String punishingModName, Instant punishingTime, String reason) {
        modLog.setGuildId(guildId);
        modLog.setUserId(userId);
        modLog.setUserName(userName);
        modLog.setPunishingModId(punishingModId);
        modLog.setPunishingModName(punishingModName);
        modLog.setPunishmentTime(punishingTime);
        modLog.setReason(reason == null ? "" : reason);
    }

    private void populate(TimedLog timedLog, long guildId, long userId, String userName, long punishingModId, String punishingModName, Instant punishingTime, Instant pardoningTime, String reason) {
        populate(timedLog, guildId, userId, userName, punishingModId, punishingModName, punishingTime, reason);
        timedLog.setPardoningTime(pardoningTime);
    }

    private ModLog getModLog0(Session session, long guildId, int caseId) {
        return session.createQuery("select m from ModLog m where m.guildId = :guildId and m.caseId = :caseId", ModLog.class)
                .setParameter("guildId", guildId)
                .setParameter("caseId", caseId)
                .getSingleResult();
    }

    @DoNotUseForVerifiedBots
    private int logPunish(ModLog modLog) throws PersistenceException {
        String timedType;
        if (modLog instanceof TimedLog timedLog) {
            timedType = timedLog.getClass().getCanonicalName();
        } else {
            timedType = null;
        }

        ReentrantLock lock = guildLock.get(modLog.getGuildId());
        lock.lock();
        int caseId = -1;
        try {
            caseId = database.doTransaction(session -> {
                int caseId1 = idProvider.get(modLog.getGuildId()).incrementAndGet();

                // Deal with potential messed up states where someone is logged as having an active timed punishment (ie role persist), but is not actually
                if (timedType != null) {
                    session.createMutationQuery("update " + timedType + " t set t.pardoningTime = :now, t.pardoningModId = :unknownModId where t.guildId = :guildId and t.userId = :userId and t.pardoningModId = 0")
                           .setParameter("now", Instant.now())
                           .setParameter("unknownModId", UNKNOWN_MOD_ID) //TODO: Check that unknown mod id, in general, is recognised by other parts of the program
                           .setParameter("guildId", modLog.getGuildId())
                           .setParameter("userId", modLog.getUserId())
                           .executeUpdate();
                }

                modLog.setCaseId(caseId1);
                session.persist(modLog);
                return caseId1;
            });
        } finally {
            lock.unlock();
        }

        // TODO: Make it so it doesn't look up a guild
        Guild g = database.getVortex().getJda().getGuildById(modLog.getGuildId());
        if (g != null) {
            database.getVortex().getBasicLogger().logPunish(g, modLog);
        }
        return caseId;
    }

    @DoNotUseForVerifiedBots
    private <T extends TimedLog> T logPardon(Class<T> clazz, long guildId, long userId, long pardoningModId, String pardoningModName, Instant pardoningTime) throws PersistenceException {
        T timedLog = database.doTransaction(session -> {
            try {
                T modLog = session.createQuery("select t from TimedLog t where type(t) = :type and t.guildId = :guildId and t.userId = :userId and t.pardoningModId = 0", clazz)
                        .setParameter("type", clazz)
                        .setParameter("guildId", guildId)
                        .setParameter("userId", userId)
                        .getSingleResult();

                modLog.setPardoningModId(pardoningModId);
                modLog.setPardoningModName(pardoningModName);
                modLog.setPardoningTime(pardoningTime);
                session.merge(modLog);

                return modLog;
            } catch (NoSuchElementException | NoResultException e) {
                return null;
            }
        });

        // TODO: Make it so it doesn't look up a guild
        if (timedLog != null) {
            Guild g = database.getVortex().getJda().getGuildById(timedLog.getGuildId());
            if (g != null) {
                database.getVortex().getBasicLogger().logPardon(g, timedLog);
            }
        }

        return timedLog;
    }
}