package com.jagrosh.vortex.hibernate.api;

import com.jagrosh.vortex.hibernate.entities.ModLog;
import com.jagrosh.vortex.hibernate.entities.TimedLog;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.PersistenceException;
import java.time.Instant;

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
        database.doTransaction(session -> {
            ModLog modlog = new ModLog();
           session.get(ModLog.class, modlog);
        });
    }

    /**
     * Deletes a modlog case from the database.
     * Note: If the modlog is an instance of {@link TimedLog},
     * then the user will be pardoned from their punishment
     *
     * @param deletingModId The ID of the moderator deleting the log
     * @param guildId The ID of the guild
     * @param caseId  The case ID to delete
     * @return A {@link ModLog} object representing the deleted case, or {@code null} if the case was not found
     * @throws PersistenceException If something went wrong while deleting the case
     */
    public ModLog deleteCase(long deletingModId, long guildId, int caseId) throws PersistenceException {
        database.doTransaction(session -> {

        });
    }

    /**
     * Edits a reason for a modlog case
     *
     * @param guildId The ID of the guild
     * @param caseId The ID of the case to edit
     * @param newReason The new reason
     * @return The old reason if successfully edited, or null if the case was not found
     * @throws PersistenceException If something went wrong while editing the reason
     */
    public String editReason(long guildId, int caseId, String newReason) throws PersistenceException {
        return null;
    }

    /**
     * Logs a warning to the database
     *
     * @param guildId The ID of the guild
     * @param userId The ID of the user
     * @param punishingModId The ID of the punishing moderator
     * @param punishingTime The time of the warning
     * @param reason The reason for the warning, or {@code null} if no reason was specified
     * @throws PersistenceException if something went wrong while logging the warning
     */
    public void logWarning(long guildId, long userId, long punishingModId, Instant punishingTime, String reason) throws PersistenceException {}

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
    public void logKick(long guildId,long userId, long punishingModId, Instant punishingTime, String reason) throws PersistenceException {

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
    public void logBan(long guildId, int caseId, long userId, long punishingModId, Instant punishingTime, Instant pardoningTime, String reason) throws PersistenceException {

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
    public void logGravel(long guildId, int caseId, long userId, long punishingModId, Instant punishingTime, Instant pardoningTime, String reason) throws PersistenceException {

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
    public void logMute(long guildId, int caseId, long userId, long punishingModId, Instant punishingTime, Instant pardoningTime, String reason) throws PersistenceException {

    }

    /**
     * Logs an unban to the database
     *
     * @param guildId The ID of the guild
     * @param userId The ID of the user
     * @param pardoningModId The ID of the unbanning mod
     * @param pardoningTime The time the user was unbanned
     */
    public void logUnban(long guildId, long userId, long pardoningModId, Instant pardoningTime) {

    }

    /**
     * Logs an ungravel to the database
     *
     * @param guildId The ID of the guild
     * @param userId The ID of the user
     * @param pardoningModId The ID of the ungraveling mod
     * @param pardoningTime The time the user was ungraveled
     */
    public void logUngravel(long guildId, long userId, long pardoningModId, Instant pardoningTime) {

    }

    /**
     * Logs an unmute to the database
     *
     * @param guildId The ID of the guild
     * @param userId The ID of the user
     * @param pardoningModId The ID of the unmuting mod
     * @param pardoningTime The time the user was unmuted
     */
    public void logUnmute(long guildId, long userId, long pardoningModId, Instant pardoningTime) {

    }
}