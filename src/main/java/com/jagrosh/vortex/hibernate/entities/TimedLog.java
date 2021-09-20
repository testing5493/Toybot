package com.jagrosh.vortex.hibernate.entities;

import com.jagrosh.vortex.hibernate.entities.BanLog;
import com.jagrosh.vortex.hibernate.entities.GravelLog;
import com.jagrosh.vortex.hibernate.entities.ModLog;
import com.jagrosh.vortex.hibernate.entities.MuteLog;
import lombok.*;

import javax.persistence.Column;

/**
 * A type of {@link ModLog} that has duration for the punishment, such as a ban, gravel or mute which
 * are represented by {@link BanLog}, {@link GravelLog}, and {@link MuteLog} respectively.
 */
@Data @NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TimedLog extends ModLog {
    /**
     * The ID of the mod pardoning the user from the punishment (eg., ungraveling or unbanning them)
     * A value of 0 represents that this was done by the bot automatically.
     */
    @Column(name = "PARDONING_MOD_ID")
    protected long pardoningModId;

    /**
     * The time when the punishment is set to end or has ended at
     * {@value Long#MAX_VALUE} represents an indefinite amount of time to be punished for
     */
    @Column(name = "PARDONING_TIME_ID")
    protected long pardoningTime;

    public TimedLog(
            long guildId,
            long userId,
            long punishingModId,
            long punishingTime,
            long pardoningModId,
            long pardoningTime,
            int caseId,
            String reason
    ) {
        super(guildId, caseId, userId, punishingModId, punishingTime, reason);
        this.pardoningModId = pardoningModId;
        this.pardoningTime = pardoningTime;
    }
}