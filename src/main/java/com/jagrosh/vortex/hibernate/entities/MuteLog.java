package com.jagrosh.vortex.hibernate.entities;

import com.jagrosh.vortex.Action;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * A persistant class representing a mute modlog entry
 */
@Table(name = "MUTES")
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class MuteLog extends TimedLog {
    @Override
    public Action actionType() {
        return Action.MUTE;
    }
}