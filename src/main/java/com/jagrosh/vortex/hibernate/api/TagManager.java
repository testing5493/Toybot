package com.jagrosh.vortex.hibernate.api;

import com.jagrosh.vortex.hibernate.entities.Tag;
import com.jagrosh.vortex.hibernate.internal.TagId;
import lombok.extern.slf4j.Slf4j;

/**
 * A collection of {@link Database} methods that are in charge of dealing with tags.
 */
@Slf4j
class TagManager {
    private final Database database;

    TagManager(Database database) {
        this.database = database;
    }

    /**
     * Creates or updates the value of a tag
     *
     * @param guildId The ID of the tags guild
     * @param name The unique name of the tag
     * @param value The value of the tag
     * @return The old value, or null if the tag was just created
     * @throws IllegalArgumentException If the name is blank or null
     */
    public String update(long guildId, String name, String value) throws IllegalArgumentException {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Name must be supplied");
        }

        TagId tagId = new TagId(guildId, name);
        return database.doTransaction(session -> {
            String oldValue;
            Tag tag = session.get(Tag.class, tagId);
            if (tag != null) {
                oldValue = tag.getValue();
                tag.setValue(value);
            } else {
                oldValue = null;
                tag = new Tag();
                tag.setGuildId(guildId);
                tag.setName(name);
                tag.setValue(value);
            }

            session.merge(tag);
            return oldValue;
        });
    }

    /**
     * Deletes a tag
     *
     * @param guildId The ID of the tags guild
     * @param name The unique name of the tag
     * @return The old value, or null if the tag doesn't exist
     */
    public String delete(long guildId, String name) {
        TagId tagId = new TagId(guildId, name);
        return database.doTransaction(session -> {
            Tag tag = session.get(Tag.class, tagId);
            if (tag == null) {
                return null;
            }

            String value = tag.getValue();
            session.remove(tag);
            return value;
        });
    }
}
