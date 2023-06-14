package com.jagrosh.vortex.hibernate.internal;

import lombok.Value;

import java.io.Serializable;

@Value
public class TagId implements Serializable {
    long guildId;
    String name;
}
