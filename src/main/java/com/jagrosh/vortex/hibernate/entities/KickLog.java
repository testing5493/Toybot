package com.jagrosh.vortex.hibernate.entities;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Table;

/**
 * A persistant class representing a kick modlog entry
 */
@Table(name = "KICKS")
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class KickLog extends ModLog {
    public KickLog(long guildId, int caseId, long userId, long punishingModId, long punishingTime, String reason) {
        super(guildId, caseId, userId, punishingModId, punishingTime, reason);
    }
}