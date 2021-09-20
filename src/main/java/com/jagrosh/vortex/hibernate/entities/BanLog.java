package com.jagrosh.vortex.hibernate.entities;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Table;

/**
 * A persistant class representing a ban modlog entry
 */
@Table(name = "BANS")
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class BanLog extends TimedLog {
    public BanLog (
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