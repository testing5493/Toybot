package com.jagrosh.vortex.hibernate.entities;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Table;

/**
 * A persistant class representing a warn modlog entry
 */
@Table(name = "WARNINGS")
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class WarnLog extends ModLog {
    public WarnLog(long guildId, int caseId, long userId, long punishingModId, long punishingTime, String reason) {
        super(guildId, caseId, userId, punishingModId, punishingTime, reason);
    }
}