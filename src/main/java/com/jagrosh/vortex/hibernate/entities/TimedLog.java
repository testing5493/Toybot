package com.jagrosh.vortex.hibernate.entities;

import com.jagrosh.vortex.Action;
import com.jagrosh.vortex.hibernate.internal.PreciseToSecondInstantConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;

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
     * The ID of the mod pardoning the user from the punishment (eg., ungraveling or unbanning them).
     * A value of 0 represents that this was done by the bot automatically, or the user has not been pardoned yet.
     */
    @Column(name = "PARDONING_MOD_ID", nullable = false)
    private long pardoningModId;

    /**
     * The username of the mod pardoning the user from the punishment (eg., ungraveling or unbanning them)
     * A value of n represents that this was done by the bot automatically.
     * Value may be null when automatically pardoned, an unknown mod pardoned the user, or when the user has not been pardoned yet.
     */
    @Column(name = "PARDONING_MOD_NAME", nullable = true)
    private String pardoningModName;

    /**
     * The time when the punishment is set to end or has ended at
     * {@value Long#MAX_VALUE} represents an indefinite amount of time to be punished for
     */
    @Column(name = "PARDONING_TIME_ID", nullable = false)
    @Convert(converter = PreciseToSecondInstantConverter.class)
    private Instant pardoningTime;

    /**
     * Whether an end time for the punishment was specified
     * @return Whether the user is punished indefinitely
     */
    public boolean isPunishedIndefinitely() {
        // Note that converting to seconds is required as modlog times are only precise to the second, while Instant.max()
        // is precise to the nanosecond
        return pardoningTime.getEpochSecond() >= Instant.MAX.getEpochSecond();
    }

    /**
     * The corresponding punishing ction type for the modlog
     *
     * @see #pardonActionType()
     */
    @Override
    public abstract Action actionType();

    /**
     * The corresponding pardoning action type for the modlog
     *
     * @see #actionType()
     */
    public abstract Action pardonActionType();
}