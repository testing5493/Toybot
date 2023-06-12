package com.jagrosh.vortex.hibernate.entities;

import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * A persistant class representing a mute modlog entry
 */
@Table(name = "MUTES")
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class MuteLog extends TimedLog {}