package com.jagrosh.vortex.hibernate.entities;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Table;

/**
 * A persistant class representing a gravel modlog entry
 */
@Table(name="GRAVELS")
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class GravelLog extends TimedLog {
    public GravelLog(
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