package com.jagrosh.vortex.hibernate.entities;

import com.jagrosh.vortex.Action;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * A persistant class representing a warn modlog entry
 */
@EqualsAndHashCode(callSuper = true)
@Table(name="WARNS")
@NoArgsConstructor
@Entity
@Data
public class WarnLog extends ModLog {
    @Override
    public Action actionType() {
        return Action.WARN;
    }
}