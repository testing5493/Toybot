package com.jagrosh.vortex.hibernate.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * A persistant class representing a tag
 */
@Entity
@Table(name = "TAGS")
@Data
@IdClass(Tag.Id.class)
@NoArgsConstructor
public class Tag {
    /**
     * The guild ID
     */
    @jakarta.persistence.Id
    @Column(name = "GUILD_ID")
    private long guildId;

    /**
     * The name of the tag, always lowercase
     */
    @jakarta.persistence.Id
    @Column(name = "NAME")
    private String name;

    /**
     * The value of the tag
     */
    @Column(name = "VALUE")
    private String value;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Id implements Serializable {
        private long guildId;
        private String name;
    }
}