package com.jagrosh.vortex.hibernate.entities;

import com.jagrosh.vortex.Action;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * A persistant class representing a ban modlog entry
 */
@Table(name = "BANS")
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class BanLog extends TimedLog {
    @Override
    public Action actionType() {
        return Action.BAN;
    }

    @Override
    public Action pardonActionType() {
        return Action.UNBAN;
    }
}