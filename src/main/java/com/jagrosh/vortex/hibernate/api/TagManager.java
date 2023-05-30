package com.jagrosh.vortex.hibernate.api;

import com.jagrosh.vortex.hibernate.entities.Tag;
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
     * Updates the value of a tag
     *
     * @param guildId The ID of the tags guild
     * @param name    The unique name of the tag
     * @param value   The value of the tag
     * @return True if successfully updated, false if something went wrong
     */
    public boolean update(long guildId, String name, String value) {
        return database.doTransaction(session -> session.update(new Tag(guildId, name, value)));
    }

    /**
     * Deletes a tag
     *
     * @param guildId The ID of the tags guild
     * @param name    The unique name of the tag
     * @return True if successfully deleted, false if something went wrong
     */
    public boolean delete(long guildId, String name) {
        return database.doTransaction(session -> session.delete(new Tag(guildId, name, null)));
    }
}
