package com.jagrosh.vortex.hibernate.entities;

import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * A persistant class representing a gravel modlog entry
 */
@Table(name = "GRAVELS")
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class GravelLog extends TimedLog {}