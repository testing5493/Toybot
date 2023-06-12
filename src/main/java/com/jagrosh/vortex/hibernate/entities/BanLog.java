package com.jagrosh.vortex.hibernate.entities;

import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * A persistant class representing a ban modlog entry
 */
@Table(name = "BANS")
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class BanLog extends TimedLog {}