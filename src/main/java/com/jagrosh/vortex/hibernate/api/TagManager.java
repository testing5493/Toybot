package com.jagrosh.vortex.hibernate.api;

import com.jagrosh.vortex.hibernate.entities.Tag;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import jakarta.persistence.PersistenceException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A collection of {@link Database} methods that are in charge of dealing with tags.
 */
@Slf4j
public class TagManager {
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

        Tag.Id tagId = new Tag.Id(guildId, name.toLowerCase());
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
     * @throws PersistenceException If something went wrong while deleting the tag
     * @throws IllegalArgumentException If the name is blank or null
     */
    public String delete(long guildId, String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Name must be supplied");
        }

        Tag.Id tagId = new Tag.Id(guildId, name.toLowerCase());
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

    /**
     * Returns a list of tag names
     *
     * @param guildId The ID of the guild
     * @return A list of the names of the tags
     * @throws PersistenceException If something went wrong while retrieving the tags
     */
    @NotNull
    public List<String> getTags(long guildId) {
        return database.doTransaction(session -> {
            CriteriaBuilder cb = session.getCriteriaBuilder();
            CriteriaQuery<String> cq =  cb.createQuery(String.class);
            Root<Tag> root = cq.from(Tag.class);
            cq.select(root.get("name")).where(cb.equal(root.get("guildId"), guildId));
            return session.createQuery(cq).getResultStream().collect(Collectors.toList());
        });
    }

    /**
     * Gets a tag
     *
     * @param guildId The id of the guild
     * @param name The name of the tag
     * @return The value of the tag, or null if no tag with that value was found
     * @throws IllegalArgumentException If the name is blank or null
     * @throws PersistenceException If something went wrong while retrieving the tags
     */
    public String getTag(long guildId, String name) throws PersistenceException {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Name must be supplied");
        }

        Tag.Id tagId = new Tag.Id(guildId, name.toLowerCase());
        return database.doTransaction(session -> {
            Tag tag = session.get(Tag.class, tagId);
            return tag == null ? null : tag.getValue();
        });
    }
}
