package com.jagrosh.vortex.hibernate.entities;

import com.jagrosh.vortex.Action;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * A persistant class representing a gravel modlog entry
 */
@Table(name = "GRAVELS")
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class GravelLog extends TimedLog {
    @Override
    public Action actionType() {
        return Action.GRAVEL;
    }

    @Override
    public Action pardonActionType() {
        return Action.UNGRAVEL;
    }
}