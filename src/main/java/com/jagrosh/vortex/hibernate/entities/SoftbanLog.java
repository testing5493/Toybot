package com.jagrosh.vortex.hibernate.entities;

import com.jagrosh.vortex.Action;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * A persistant class representing a softban modlog entry
 */
@Table(name = "SOFTBANS")
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class SoftbanLog extends ModLog {
    @Override
    public Action actionType() {
        return Action.SOFTBAN;
    }
}