package com.jagrosh.vortex.logging;

import com.jagrosh.vortex.Constants;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.utils.FileUpload;

import java.awt.Color;
import java.time.Instant;
import java.time.temporal.TemporalAccessor;

/**
 * A class representing a modlog embed sent in the modlogs channel for auditable actions
 */
public sealed interface ModlogEmbed permits ModlogEmbedImpl {
    /**
     * Creates a modlog embed for a single guild
     */
    static ModlogEmbed createForSingleGuild() {
        return new ModlogEmbedImpl();
    }

    /**
     * Creates a modlog embed for multiple guilds. Slightly more efficient for multiple guilds compared to
     * {@link ModlogEmbed#createForSingleGuild()}
     */
    // TODO: Make this return a seperate object
    static ModlogEmbed createForMultiGuild() {
        return new ModlogEmbedImpl();
    }

    /**
     * Sets the user that this modlog "targets". If set, this will append "User Id: " to the footer and put the
     * name of the user in the author field of the embed
     * <p>
     *     Note: If you have multiple {@link UserSnowflake} implementations for the same person (Ie., a {@link net.dv8tion.jda.api.entities.Member Member}
     *     object and a {@link net.dv8tion.jda.api.entities.User User} object), always pass in the implementation that contains more data! In this case,
     *     a {@link net.dv8tion.jda.api.entities.Member Member} object would be preferred as it contains general user data AND guild specific data (such as nicknames/roles) while
     *     a {@link net.dv8tion.jda.api.entities.User User} object only contains general user data (like username and other non-guild dependant fields). This is so that
     *     if the bot needs extra data for formatting modlogs, then it won't have to waste resources lazy-loading them if they could be passed in from the start.
     * </p>
     * <p>
     *     If you only have a user ID and not the corresponding {@link net.dv8tion.jda.api.entities.Member Member} or {@link net.dv8tion.jda.api.entities.User User} objects,
     *     you should use {@link net.dv8tion.jda.api.entities.User#fromId(long) User#fromId(long)} to wrap the Id. (This method will not retrieve any other userfields
     *     and the bot will lazy-load neccessary information as needed).
     * </p>
     * @param user The user object
     * @return {@link ModlogEmbed}, for chaining convenience
     */
    ModlogEmbed setTargetUser(UserSnowflake user);

    /**
     * Sets the moderator for the modlog. If set, this will append "Moderator Id: {id}" to the footer. If a {@link #setTargetUser(UserSnowflake)} target user}
     * is not set, the moderator will have their name/avatar shown in the author field
     * <p>
     *     Note: If you have multiple {@link UserSnowflake} implementations for the same person (Ie., a {@link net.dv8tion.jda.api.entities.Member Member}
     *     object and a {@link net.dv8tion.jda.api.entities.User User} object), always pass in the implementation that contains more data! In this case,
     *     a {@link net.dv8tion.jda.api.entities.Member Member} object would be preferred as it contains general user data AND guild specific data (such as nicknames/roles) while
     *     a {@link net.dv8tion.jda.api.entities.User User} object only contains general user data (like username and other non-guild dependant fields). This is so that
     *     if the bot needs extra data for formatting modlogs, then it won't have to waste resources lazy-loading them if they could be passed in from the start.
     * </p>
     * <p>
     *     If you only have a user ID and not the corresponding {@link net.dv8tion.jda.api.entities.Member Member} or {@link net.dv8tion.jda.api.entities.User User} objects,
     *     you should use {@link net.dv8tion.jda.api.entities.User#fromId(long) User#fromId(long)} to wrap the Id. (This method will not retrieve any other userfields
     *     and the bot will lazy-load neccessary information as needed).
     * </p>

     * @param mod The user object of the moderator
     * @return {@link ModlogEmbed}, for chaining convenience
     */
    ModlogEmbed setModerator(UserSnowflake mod);

    /**
     * The title of the modlog.
     * <br>
     * Example: modLog.setTitle("Message sent by <@" + userId + "> deleted in <#" + channelId + ">");
     * @param str The text of the title
     * @return {@link ModlogEmbed}, for chaining convenience
     */
    ModlogEmbed setTitle(String str);

    /**
     * The description of the modlog. (Ie the stuff that's not bold underneath the title). Note that fields should be created using
     * {@link #addField(String, String, boolean)}.
     *
     * @return {@link ModlogEmbed}, for chaining convenience
     */
    ModlogEmbed setDescription(String description);

    /**
     * Printf style setter for the {@link #setDescription(String)}. Passing in a {@link IMentionable} will call its
     * {@link IMentionable#getAsMention() getAsMention()} method opposed to {@link Object#toString() toString}
     * @param format The string to be formatted using {@link String#format(String, Object...)}
     * @param args The arguments for the formatter
     * @implNote
     *
     * @return {@link ModlogEmbed}, for chaining convenience
     */
    default ModlogEmbed formatDescription(String format, Object... args) {
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof IMentionable mentionable) {
                    args[i] = mentionable.getAsMention();
                }
            }
        }

        return setDescription(String.format(format, args));
    }

    /**
     * Adds a field to the modlog.
     *
     * @param name The name of the field
     * @param value The value of the field
     * @param inline Whether the field will show on the same line as other fields given enough space
     * @return {@link ModlogEmbed}, for chaining convenience
     *
     * @see #addField(String, String)
     */
    ModlogEmbed addField(String name, String value, boolean inline);

    /**
     * Adds a field to the modlog. The field will be inline if the value is less than or equal to {@value ConstantsDEFAULT_MAX_INLINE} characters.
     *
     * @param name The name of the field
     * @param value The value of the field
     *
     * @see #addField(String, String, boolean)
     */
    default ModlogEmbed addField(String name, String value) {
        return addField(name, value, value != null && value.length() <= Constants.DEFAULT_MAX_INLINE);
    }

    /**
     * Sets an image for the field. Note that this method should be used for, and not one of the smaller log icons that should be
     * sent using {@link #setIcon(String)}
     * @param fileupload A {@link FileUpload}. Please do not use {@link FileUpload#fromData(java.io.InputStream, String)}, as the modlog may not be sent immediately.
     * @return {@link ModlogEmbed}, for chaining convenience
     */
    ModlogEmbed setImage(FileUpload fileupload);

    /**
     * Sets an image for the field. Note that this method should be used for, and not one of the smaller log icons that should be
     * sent using {@link #setIcon(String)}
     * @param url The image url
     * @return {@link ModlogEmbed}, for chaining convenience
     */
    ModlogEmbed setImage(String url);

    /**
     * Sets the icon for this modlog. Icon URLs should be gotten from {@link com.jagrosh.vortex.Emoji.LogEmoji Emoji.LOGS}
     * <br> Ie., modLog.setIcon(Emoji.LOGS.GRAVEL.greenIcon(false));
     * @param url The url returned by ._____Icon(false). (Note that false in this case specifies that the icon should be small)
     * @return {@link ModlogEmbed}, for chaining convenience
     */
    ModlogEmbed setIcon(String url);

    /**
     * Sets the color of the emebed, prefferably from {@link com.jagrosh.vortex.utils.ToycatPallete ToycatPallete}
     * @param color The color
     * @return {@link ModlogEmbed}, for chaining convenience
     */
    ModlogEmbed setColor(Color color);

    /**
     * Sets the timestamp of the time this modlog was created. If no timestamp is explicitally set, the modlog will use
     * the current unix timestamp.
     * @param time A {@link TemporalAccessor} instance such as {@link java.time.Instant} or {@link java.time.OffsetDateTime}
     * @return {@link ModlogEmbed}, for chaining convenience
     */
    ModlogEmbed setTimestamp(TemporalAccessor time);

    /**
     * Sets the timestamp of the time this modlog was created. If no timestamp is explicitally set, the modlog will use
     * the current unix timestamp.
     * @param unixEpochTimestampMilli A unix epoch timestamp in milliseconds
     * @return {@link ModlogEmbed}, for chaining convenience
     */
    default ModlogEmbed setTimestamp(long unixEpochTimestampMilli) {
        return setTimestamp(Instant.ofEpochMilli(unixEpochTimestampMilli));
    }

    /**
     * Appends an ID to the footer of the modlog. <em>Note that {@link #setTargetUser(UserSnowflake)} (long)}</em> and {@link #setModerator(UserSnowflake)} (long)}
     * already append their respective IDs</em>. Example:
     * <pre>{@code
     * modLog.setTargetUser(1000003400304343)
     *       .setModerator(23429342000303344)
     *       .appendIdToFooter("VC Channel", 49230149234023943);
     * }</pre>
     * Will result in "User ID: 1000003400304343 | Mod ID: 23429342000303344 | VC Channel ID: 49230149234023943"
     * being generated
     * @param objectName The name of the object. Omit "ID"
     * @param id The id of the object
     * @return {@link ModlogEmbed}, for chaining convenience
     */
    ModlogEmbed appendIdToFooter(String objectName, long id);

    /**
     * Appends an ID to the footer of the modlog. <em>Note that {@link #setTargetUser(UserSnowflake)} (long)}</em> and {@link #setModerator(UserSnowflake)} (long)}
     * already append their respective IDs</em>. Example:
     * <pre>{@code
     * modLog.setTargetUser(userObject)
     *       .setModerator(modMember)
     *       .appendIdToFooter("VC Channel", vcChannel);
     * }</pre>
     * Will result in "User ID: 1000003400304343 | Mod ID: 23429342000303344 | VC Channel ID: 49230149234023943"
     * being generated
     * @param objectName The name of the object. Omit "ID"
     * @param id An {@link ISnowflake}
     * @return {@link ModlogEmbed}, for chaining convenience
     */
    default ModlogEmbed appendIdToFooter(String objectName, ISnowflake id) {
        return appendIdToFooter(objectName, id.getIdLong());
    }
}
