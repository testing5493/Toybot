package com.jagrosh.vortex.hibernate.api;

import com.jagrosh.vortex.hibernate.entities.*;
import com.jagrosh.vortex.hibernate.internal.ModLogId;
import jakarta.persistence.PersistenceException;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;

import java.util.NoSuchElementException;

/**
 * A collection of {@link Database} methods that are in charge of dealing with modlogs
 */
@Slf4j
class ModlogManager {
    private final Database database;

    ModlogManager(Database database) {
        this.database = database;
    }

    /**
     * Gets a modlog case from the database
     *
     * @param guildId The guild ID
     * @param caseId The case ID
     * @return A Modlog object, or {@code null} if no log was found
     */
    public ModLog getCase(long guildId, int caseId) throws PersistenceException {
        return database.doTransaction(session -> {
            return getModLog0(session, guildId, caseId);
        });
    }

    /**
     * Deletes a modlog case from the database.
     * Note: If the modlog is an instance of {@link TimedLog},
     * then the user will be pardoned from their punishment
     *
     * @param deletingModId The ID of the moderator deleting the log
     * @param guildId The ID of the guild
     * @param caseId The case ID to delete
     * @return A {@link ModLog} object representing the deleted case, or {@code null} if the case was not found
     * @throws PersistenceException If something went wrong while deleting the case
     */
    public ModLog deleteCase(long deletingModId, long guildId, int caseId) throws PersistenceException {
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
    public String editReason(long guildId, int caseId, String reason) throws PersistenceException {
        ModLogId modLogId = new ModLogId(guildId, caseId);
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
     * @param punishingModId The ID of the punishing moderator
     * @param punishingTime The time of the warning
     * @param reason The reason for the warning, or {@code null} if no reason was specified
     * @return The caseId
     * @throws PersistenceException if something went wrong while logging the warning
     */
    public int logWarning(long guildId, long userId, long punishingModId, long punishingTime, String reason) throws PersistenceException {
        WarnLog warnLog = new WarnLog();
        populate(warnLog, guildId, userId, punishingModId, punishingTime, reason);
        return logPunish(warnLog);
    }

    /**
     * Logs a kick to the database
     *
     * @param guildId The ID of the guild
     * @param userId The ID of the user
     * @param punishingModId The ID of the punishing moderator
     * @param punishingTime The time of the kick
     * @param reason The reason for the kick, or {@code null} if no reason was specified
     * @throws PersistenceException if something went wrong while logging the kick
     */
    public int logKick(long guildId, long userId, long punishingModId, long punishingTime, String reason) throws PersistenceException {
        KickLog kickLog = new KickLog();
        populate(kickLog, guildId, userId, punishingModId, punishingTime, reason);
        return logPunish(kickLog);
    }

    /**
     * Logs a ban to the database
     *
     * @param guildId The ID of the guild
     * @param userId The ID of the user
     * @param punishingModId The ID of the punishing moderator
     * @param punishingTime The time of the punishment
     * @param pardoningTime The time the user will be auto unbanned, or {@code null} if the user is banned indefinitely
     * @param reason The reason for the ban, or {@code null} if no reason was specified
     * @throws PersistenceException if something went wrong while logging the ban
     */
    public int logBan(long guildId, long userId, long punishingModId, long punishingTime, long pardoningTime, String reason) throws PersistenceException {
        BanLog banLog = new BanLog();
        populate(banLog, guildId, userId, punishingModId, punishingTime, pardoningTime, reason);
        return logPunish(banLog);
    }

    /**
     * Logs a gravel to the database
     *
     * @param guildId The ID of the guild
     * @param userId The ID of the user
     * @param punishingModId The ID of the punishing moderator
     * @param punishingTime The time of the punishment
     * @param pardoningTime The time the user will be auto ungraveled, or {@code null} if the user is graveled indefinitely
     * @param reason The reason for the gravel, or {@code null} if no reason was specified
     * @throws PersistenceException if something went wrong while logging the gravel
     */
    public int logGravel(long guildId, long userId, long punishingModId, long punishingTime, long pardoningTime, String reason) throws PersistenceException {
        GravelLog gravelLog = new GravelLog();
        populate(gravelLog, guildId, userId, punishingModId, punishingTime, pardoningTime, reason);
        return logPunish(gravelLog);
    }

    /**
     * Logs a mute to the database
     *
     * @param guildId The ID of the guild
     * @param userId The ID of the user
     * @param punishingModId The ID of the punishing moderator
     * @param punishingTime The time of the punishment
     * @param pardoningTime The time the user will be auto unmuted, or {@code null} if the user is muted indefinitely
     * @param reason The reason for the gravel, or {@code null} if no reason was specified
     * @throws PersistenceException if something went wrong while logging the mute
     */
    public int logMute(long guildId, long userId, long punishingModId, long punishingTime, long pardoningTime, String reason) throws PersistenceException {
        MuteLog muteLog = new MuteLog();
        populate(muteLog, guildId, userId, punishingModId, punishingTime, pardoningTime, reason);
        return logPunish(muteLog);
    }

    /**
     * Logs an unban to the database
     *
     * @param guildId The ID of the guild
     * @param userId The ID of the user
     * @param pardoningModId The ID of the unbanning mod
     * @param pardoningTime The time the user was unbanned
     */
    public BanLog logUnban(long guildId, long userId, long pardoningModId, long pardoningTime) {
        return logPardon(BanLog.class, guildId, userId, pardoningModId, pardoningTime);
    }

    /**
     * Logs an ungravel to the database
     *
     * @param guildId The ID of the guild
     * @param userId The ID of the user
     * @param pardoningModId The ID of the ungraveling mod
     * @param pardoningTime The time the user was ungraveled
     */
    public GravelLog logUngravel(long guildId, long userId, long pardoningModId, long pardoningTime) {
        return logPardon(GravelLog.class, guildId, userId, pardoningModId, pardoningTime);
    }

    /**
     * Logs an unmute to the database
     *
     * @param guildId The ID of the guild
     * @param userId The ID of the user
     * @param pardoningModId The ID of the unmuting mod
     * @param pardoningTime The time the user was unmuted
     */
    public MuteLog logUnmute(long guildId, long userId, long pardoningModId, long pardoningTime) {
        return logPardon(MuteLog.class, guildId, userId, pardoningModId, pardoningTime);
    }

    private void populate(ModLog modLog, long guildId, long userId, long punishingModId, long punishingTime, String reason) {
        modLog.setGuildId(guildId);
        modLog.setUserId(userId);
        modLog.setPunishingModId(punishingModId);
        modLog.setPunishmentTime(punishingTime);
        modLog.setReason(reason == null ? "" : reason);
    }

    private void populate(TimedLog timedLog, long guildId, long userId, long punishingModId, long punishingTime, long pardoningTime, String reason) {
        populate(timedLog, guildId, userId, punishingModId, punishingTime, reason);
        timedLog.setPardoningTime(pardoningTime);
    }

    private ModLog getModLog0(Session session, long guildId, int caseId) {
        return session.createQuery("select m from ModLog m where m.guildId = :guildId and m.caseId = :caseId", ModLog.class)
                .setParameter("guildId", guildId)
                .setParameter("caseId", caseId)
                .getSingleResult();
    }

    private int logPunish(ModLog modLog) {
        return database.doTransaction(session -> (ModLog) session.save(modLog)).getCaseId();
    }

    private <T extends TimedLog> T logPardon(Class<T> clazz, long guildId, long userId, long pardoningModId, long pardoningTime) {
        return database.doTransaction(session -> {
            try {
                T modLog = session.createQuery("select t from TimedLog t where type(t) = :type and t.guildId = :guildId and t.userId = :userId and t.pardoningModId = 0", clazz)
                                  .setParameter("type", clazz.getName())
                                  .setParameter("guildId", guildId)
                                  .setParameter("userId", userId)
                                  .getSingleResult();

                modLog.setPardoningModId(pardoningModId);
                modLog.setPardoningTime(pardoningTime);
                session.merge(modLog);

                return modLog;
            } catch (NoSuchElementException e) {
                return null;
            }
        });
    }
}