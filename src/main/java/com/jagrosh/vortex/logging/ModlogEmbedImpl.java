package com.jagrosh.vortex.logging;

import com.jagrosh.vortex.utils.FormatUtil;
import com.jagrosh.vortex.utils.OtherUtil;
import com.jagrosh.vortex.utils.ToycatPallete;
import lombok.Getter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.utils.FileUpload;

import java.awt.Color;
import java.time.Instant;
import java.time.temporal.TemporalAccessor;
import java.util.LinkedList;
import java.util.List;

public non-sealed class ModlogEmbedImpl implements ModlogEmbed {
    private UserSnowflake targetUserSnowflake, modUserSnowflake;
    private String title, description;
    @Getter
    private FileUpload fileUpload;
    private Color color;
    private TemporalAccessor time;
    private String imageUrl, iconUrl;
    private List<MessageEmbed.Field> embedFields = null;
    private final List<String> footerList = new LinkedList<>();

    public ModlogEmbedImpl() {}

    @Override
    public ModlogEmbed setTargetUser(UserSnowflake userOrMember) {
        this.targetUserSnowflake = userOrMember;
        return this;
    }

    @Override
    public ModlogEmbed setModerator(UserSnowflake userOrMember) {
        this.modUserSnowflake = userOrMember;
        return this;
    }

    @Override
    public ModlogEmbed setTitle(String str) {
        this.title = FormatUtil.clamp(str, MessageEmbed.TITLE_MAX_LENGTH);
        return this;
    }

    @Override
    public ModlogEmbed setDescription(String description) {
        this.description = FormatUtil.clamp("### " + description, MessageEmbed.DESCRIPTION_MAX_LENGTH);
        return this;
    }

    @Override
    public ModlogEmbed addField(String name, String value, boolean inline) {
        if (embedFields == null) {
            embedFields = new LinkedList<>();
        }

        embedFields.add(new MessageEmbed.Field(FormatUtil.clamp(name, 50), FormatUtil.clamp(value, MessageEmbed.VALUE_MAX_LENGTH), inline)); // TODO: Find out actual max length for field names
        return this;
    }


    @Override
    public ModlogEmbed setImage(FileUpload fileUpload) {
        this.fileUpload = fileUpload;
        return this;
    }

    @Override
    public ModlogEmbed setImage(String url) {
        this.imageUrl = url;
        return this;
    }

    @Override
    public ModlogEmbed setIcon(String url) {
        this.iconUrl = url;
        return this;
    }

    @Override
    public ModlogEmbed setColor(Color color) {
        this.color = color;
        return this;
    }

    @Override
    public ModlogEmbed setTimestamp(TemporalAccessor time) {
        this.time = time;
        return this;
    }

    @Override
    public ModlogEmbed appendIdToFooter(String objectName, long id) {
        footerList.add(formatFooter(objectName, id));
        return this;
    }

    // TODO: Send another embed when there are 0 chars remaining instead of trunctating
    public MessageEmbed build(Guild g) {


        int charRemaining = MessageEmbed.EMBED_MAX_LENGTH_BOT;
        EmbedBuilder builder = new EmbedBuilder().setColor(color == null ? ToycatPallete.DARK_BLUE : color)
                                                 .setThumbnail(iconUrl)
                                                 .setTimestamp(time != null ? time : Instant.now());


        if (targetUserSnowflake != null) {
            charRemaining -= setAuthor(builder, g, targetUserSnowflake);
        } else if (modUserSnowflake != null) {
            charRemaining -= setAuthor(builder, g, modUserSnowflake);
        }


        if (imageUrl != null) {
            builder.setImage(imageUrl);
        } else if (fileUpload != null) {
            builder.setImage("attachment://" + fileUpload.getName());
        }


        if (title != null && !title.isEmpty()) {
            builder.setTitle(title);
            charRemaining -= title.length();
        }


        if (description != null) {
            builder.setDescription(description);
            charRemaining -= description.length();
        }


        if (modUserSnowflake != null) {
             footerList.add(0, formatFooter("Mod", modUserSnowflake.getIdLong()));
        }
        if (targetUserSnowflake != null) {
            footerList.add(0, formatFooter("User", targetUserSnowflake.getIdLong()));
        }
        String footer = FormatUtil.clamp(FormatUtil.formatList(footerList, " | "), Math.min(2048, charRemaining)); //TODO: Add 2048 max length for footers to JDA
        builder.setFooter(footer);
        charRemaining -= footer.length();
        if (charRemaining == 0) { // At this point we may start to exhaust all our characters, although it is unlikely
            return builder.build();
        }

        if (embedFields != null) {
            for (MessageEmbed.Field field : embedFields) {
                String newValue = null, newName = null;
                if (field.getValue() != null) {
                    newValue = FormatUtil.clamp(field.getValue(), charRemaining);
                    charRemaining -= newValue.length();
                }

                if (field.getName() != null) {
                    newName = FormatUtil.clamp(field.getName(), charRemaining);
                    charRemaining -= newName.length();
                }

                if (charRemaining == 0) {
                    builder.addField(new MessageEmbed.Field(newName, newValue, field.isInline()));
                    return builder.build();
                } else {
                    builder.addField(field);
                }
            }
        }

        return builder.build();
    }

    private int setAuthor(EmbedBuilder builder, Guild g, UserSnowflake userSnowflake) {
        userSnowflake = OtherUtil.getMostRelevent(g, userSnowflake);

        String username, discrim, nickname, avatar;
        switch (userSnowflake) {
            case Member m -> {
                username = m.getUser().getName();
                discrim  = m.getUser().getDiscriminator();
                nickname = m.getEffectiveName();
                avatar   = m.getEffectiveAvatarUrl();
            }
            case User u -> {
                username = u.getName();
                discrim  = u.getDiscriminator();
                nickname = u.getEffectiveName();
                avatar   = u.getEffectiveAvatarUrl();
            }
            default -> {
                username = "Unknown User";
                discrim = null;
                nickname = null;
                avatar = userSnowflake.getDefaultAvatarUrl();
            }
        }


        String loggingName = FormatUtil.formatUser(username, discrim);

        // If the user has a nickname, add that to the end of the name in parentheses
        if (nickname != null && !username.equals(nickname)) {
            loggingName = String.format("%s (%s)", loggingName, nickname.isBlank() ? " " : nickname);
        }

        loggingName = FormatUtil.clamp(loggingName, MessageEmbed.AUTHOR_MAX_LENGTH);
        builder.setAuthor(loggingName, null, avatar);
        return loggingName.length();
    }

    private String formatFooter(String objectName, long id) {
        return objectName + " ID: " + id;
    }
}
