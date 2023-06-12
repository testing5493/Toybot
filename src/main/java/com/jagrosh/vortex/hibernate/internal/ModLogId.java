package com.jagrosh.vortex.hibernate.internal;

import lombok.Value;

import java.io.Serializable;

@Value
public class ModLogId implements Serializable {
    long guildId;
    int caseId;
}
