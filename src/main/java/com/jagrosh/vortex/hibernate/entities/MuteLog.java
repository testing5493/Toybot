package com.jagrosh.vortex.hibernate.entities;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Table;

/**
 * A persistant class representing a mute modlog entry
 */
@Table(name="MUTES")
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class MuteLog extends TimedLog {
    public MuteLog(
            long guildId,
            long userId,
            long punishingModId,
            long punishingTime,
            long pardoningModId,
            long pardoningTime,
            int caseId,
            String reason
    ) {
        super(guildId, userId, punishingModId, punishingTime, pardoningModId, pardoningTime, caseId, reason);
    }
}