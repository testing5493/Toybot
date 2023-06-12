package com.jagrosh.vortex.hibernate.entities;

import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * A persistant class representing a kick modlog entry
 */
@Table(name = "KICKS")
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class KickLog extends ModLog {}