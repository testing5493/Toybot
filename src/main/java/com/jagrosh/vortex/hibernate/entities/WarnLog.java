package com.jagrosh.vortex.hibernate.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * A persistant class representing a warn modlog entry
 */
@Table(name = "WARNINGS")
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Entity
@Data
public class WarnLog extends ModLog {}