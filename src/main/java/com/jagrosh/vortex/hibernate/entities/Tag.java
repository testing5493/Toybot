package com.jagrosh.vortex.hibernate.entities;

import lombok.*;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * A persistant class representing a tag
 */
@Entity @Table(name="TAGS")
@Data @NoArgsConstructor
@AllArgsConstructor
public class Tag {
    /** The guild ID */
    @Id @Column(name = "GUILD_ID")
    private long guildId;

    /** The name of the tag, always lowercase */
    @Id @Column(name = "NAME")
    private String name;

    /** The value of the tag */
    @Column(name = "VALUE")
    private String value;
}