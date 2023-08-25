package com.jagrosh.vortex.hibernate.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * A type of {@link ModLog} that has duration for the punishment, such as a ban, gravel or mute which
 * are represented by {@link BanLog}, {@link GravelLog}, and {@link MuteLog} respectively.
 */
@Data
@Entity
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public abstract class TimedLog extends ModLog {
    /**
     * The ID of the mod pardoning the user from the punishment (eg., ungraveling or unbanning them)
     * A value of 0 represents that this was done by the bot automatically.
     */
    @Column(name = "PARDONING_MOD_ID")
    private long pardoningModId;

    /**
     * The time when the punishment is set to end or has ended at
     * {@value Long#MAX_VALUE} represents an indefinite amount of time to be punished for
     */
    @Column(name = "PARDONING_TIME_ID")
    private long pardoningTime;
}