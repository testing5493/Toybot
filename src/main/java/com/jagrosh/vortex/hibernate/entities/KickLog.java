package com.jagrosh.vortex.hibernate.entities;

import com.jagrosh.vortex.Action;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * A persistant class representing a kick modlog entry
 */
@Table(name = "KICKS")
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class KickLog extends ModLog {
    @Override
    public Action actionType() {
        return Action.KICK;
    }
}