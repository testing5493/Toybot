/*
 * Copyright 2017 John Grosh (john.a.grosh@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jagrosh.vortex.logging;

import com.jagrosh.vortex.Emoji;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.database.Database.Modlog;
import com.jagrosh.vortex.logging.MessageCache.CachedMessage;
import com.jagrosh.vortex.utils.*;
import com.typesafe.config.Config;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import net.dv8tion.jda.api.audit.AuditLogKey;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateAvatarEvent;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.Temporal;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author John Grosh (john.a.grosh@gmail.com)
 */
// TODO: Make sure all values are clamped
// TODO: Check if bot can speak in a modlogs channel before creating the message
public class ModlogGenerator {
    private final Vortex vortex;
    private final AvatarSaver avatarSaver;

    public ModlogGenerator(Vortex vortex, Config config) {
        this.vortex = vortex;
        this.avatarSaver = new AvatarSaver(config);
    }

    /* Message Logs */
    public void logMessageEdit(Message newMessage, CachedMessage oldMessage) {
        if (oldMessage == null || newMessage.getContentRaw().equals(oldMessage.getContentRaw()) || !newMessage.isEdited()) {
            return;
        }

        Guild g = newMessage.getGuild();
        TextChannel modlogsChannel = getModlogsChannel(g);
        if (modlogsChannel == null) {
            return;
        }

        UserSnowflake author = newMessage.getMember() == null ? newMessage.getAuthor() : newMessage.getMember();
        long channelId = newMessage.getChannel().getIdLong();

        ModlogEmbed modlogEmbed = ModlogEmbed.createForSingleGuild()
                .setTargetUser(author)
                .formatDescription("**Message edited in <#%s> [Jump to Message](https://discord.com/channels/%s/%s/%s)**",
                        channelId,
                        g.getId(),
                        channelId,
                        newMessage.getId()
                ).appendIdToFooter("Message", newMessage.getIdLong())
                .addField("Before", FormatUtil.formatMessage(oldMessage), false) // TODO: Ditch FormatUtil#formatMessage
                .addField("After", FormatUtil.formatMessage(newMessage), false)
                .setColor(DiscordPallete.BLURPLE) // TODO: Replace with discord pallete colours
                .setIcon(Emoji.LOGS.EDIT.blurpleIcon(false))
                .appendIdToFooter("Channel", channelId)
                .appendIdToFooter("Message", newMessage.getIdLong())
                .setTimestamp(newMessage.getTimeEdited());

        log(modlogsChannel, modlogEmbed);
    }

    public void logMessageDelete(CachedMessage oldMessage) {
        if (oldMessage == null) {
            return;
        }

        Guild guild = oldMessage.getGuild(vortex.getJda());
        if (guild == null) {
            return;
        }

        TextChannel modlogsChannel = getModlogsChannel(guild);
        if (modlogsChannel == null) {
            return;
        }

        String formatted = FormatUtil.formatMessage(oldMessage);
        if (formatted.isEmpty()) {
            return;
        }

        ModlogEmbed modlogEmbed = ModlogEmbed.createForSingleGuild()
                .setColor(DiscordPallete.YELLOW)
                .setIcon(Emoji.LOGS.DELETE.yellowIcon(false))
                .setTargetUser(User.fromId(oldMessage.getAuthorId()))
                .formatDescription("**Message sent by <@%d> deleted in <#%d>**%n%s", oldMessage.getAuthorId(), oldMessage.getTextChannelId(), formatted)
                .appendIdToFooter("Message", oldMessage.getIdLong())
                .appendIdToFooter("Channel", oldMessage.getTextChannelId());

        log(modlogsChannel, modlogEmbed);
    }

    public void logMessageBulkDelete(List<CachedMessage> messages, int count, TextChannel text) {
        if (count == 0 || messages.isEmpty()) {
            return;
        } else if (messages.size() == 1) {
            logMessageDelete(messages.get(0));
            return;
        }

        Guild guild = text.getGuild();
        TextChannel modlogsChannel = getModlogsChannel(guild);
        if (modlogsChannel == null) {
            return;
        }

        vortex.getTextUploader().upload(
              LogUtil.logCachedMessagesForwards("Deleted Messages", messages, vortex.getJda()),
              "DeletedMessages",
              (view, download) -> {
                  ModlogEmbed modlogEmbed = ModlogEmbed.createForSingleGuild()
                          .setColor(DiscordPallete.RED)
                          .setIcon(Emoji.LOGS.DELETE.redIcon(false))
                          .formatDescription("**Bulk delete in %s, %d messages were deleted**%nClick to [view](%s) or [download](%s) the deleted messages", text.getAsMention(), count, view, download)
                          .appendIdToFooter("Channel", text.getIdLong());

                  log(modlogsChannel, modlogEmbed);
              }
        );
    }

    public void postCleanCase(Member moderator, OffsetDateTime now, int count, TextChannel target, String criteria, String reason, String view, String download) {
        TextChannel modlogsChannel = getModlogsChannel(moderator.getGuild());
        if (modlogsChannel == null) {
            return;
        }

        boolean plural = count != 1;
        ModlogEmbed modlogEmbed = ModlogEmbed.createForSingleGuild()
                .setColor(DiscordPallete.RED)
                .setIcon(Emoji.LOGS.PURGE.redIcon(false))
                .setModerator(moderator)
                .formatDescription("**%s purged %d message%s from %s**%n**Criteria:** %s%n%sClick to [view](%s) or [download](%s) the deleted message%s.",
                        moderator.getAsMention(),
                        count,
                        plural ? "s" : "",
                        target.getAsMention(),
                        criteria,
                        reason == null || reason.trim().isEmpty() ? "" : "**Reason:** " + reason + "\n",
                        view,
                        download,
                        plural ? "s" : ""
                ).appendIdToFooter("Channel", target.getIdLong())
                .setTimestamp(now);

        log(modlogsChannel, modlogEmbed);
    }

    //TODO: Will be worked on in the future
    public void logRedirectPath(Message message, String link, List<String> redirects) {
        /*TextChannel tc = vortex.getDatabase().settings.getSettings(message.getGuild()).getMessageLogChannel(message.getGuild());
        if (tc == null) {
            return;
        }

        StringBuilder sb = new StringBuilder(REDIR_END + " **" + link + "**");
        for (int i = 0; i < redirects.size(); i++) {
            sb.append("\n").append(redirects.size() - 1 == i ? REDIR_END + " **" : REDIR_MID).append(redirects.get(i)).append(redirects.size() - 1 == i ? "**" : "");
        }

        // log(OffsetDateTime.now(), tc, REDIRECT, FormatUtil.formatFullUser(message.getAuthor()) + "'s message in " + message.getChannel().getAsMention() + " contained redirects:", new EmbedBuilder().setColor(Color.BLUE.brighter().brighter()).appendDescription(sb.toString()).build());
         */
    }

    public void logNameChange(User user, String oldUsername, String newUsername) {
        ModlogEmbed modlogEmbed = ModlogEmbed.createForMultiGuild()
                .setTargetUser(user)
                .formatDescription("**%s has changed their username from %s to %s**", oldUsername, newUsername)
                .setColor(DiscordPallete.BLURPLE)
                .setIcon(Emoji.LOGS.PROFILE_EDIT.blueIcon(false));

        logToMutualGuilds(user, modlogEmbed);
    }

    public void logGuildJoin(GuildMemberJoinEvent event, OffsetDateTime now) {
        TextChannel modlogsChannel = getModlogsChannel(event.getGuild());
        if (modlogsChannel == null) {
            return;
        }


        Member m = event.getMember();
        long accountAge = m.getTimeJoined().toInstant().toEpochMilli() - m.getTimeCreated().toInstant().toEpochMilli();

        ModlogEmbed modlogEmbed = ModlogEmbed.createForSingleGuild()
                .setTargetUser(m)
                .setTimestamp(m.getTimeJoined());

        if (accountAge <= 0) {
            modlogEmbed.formatDescription("**%s joined the server**%nThis account has joined \"before\" being created and is definitely an alt.", m.getAsMention())
                    .setColor(DiscordPallete.RED)
                    .setIcon(Emoji.LOGS.JOIN_ALT.yellowIcon(false));
        } else {
            modlogEmbed.formatDescription("**%s joined the server**%nAccount created on %s (%s)",
                            m.getAsMention(),
                            TimeFormat.DATE_TIME_SHORT.format(m.getTimeJoined()),
                            TimeFormat.RELATIVE.format(m.getTimeJoined()))
                    .setColor(DiscordPallete.YELLOW)
                    .setIcon(Emoji.LOGS.JOIN.greenIcon(false));
        }

        log(modlogsChannel, modlogEmbed);
    }

    public void logGuildLeave(GuildMemberRemoveEvent event) {
        Guild g = event.getGuild();
        User u = event.getUser();
        OffsetDateTime now = OffsetDateTime.now();
        Member m = event.getMember();


        EmbedBuilder builder = new EmbedBuilder().setAuthor(getLoggingName(g, u), null, u.getEffectiveAvatarUrl())
                                                 .setThumbnail(u.getEffectiveAvatarUrl())
                                                 .setColor(Color.BLUE)
                                                 .appendDescription("**" + u.getAsMention() + " left or was kicked/banned from the server**")
                                                 .setFooter("User ID: " + u.getId(), null)
                                                 .setTimestamp(now);

        if (m != null) {
            if (!m.getRoles().isEmpty()) {
                builder.addField("Roles", FormatUtil.listOfRolesMention(m.getRoles()), false);
            }

            builder.addField("Joined", TimeFormat.DATE_TIME_SHORT.format(m.getTimeJoined()), false);
        }

        log(g, builder);
    }

    public void logVoiceUpdate(GuildVoiceUpdateEvent event) {
        if (event.getChannelJoined() != null && event.getChannelLeft() != null) {
            logVoiceMove(event);
        } else if (event.getChannelJoined() != null) {
            logVoiceJoin(event);
        } else {
            logVoiceLeave(event);
        }
    }

    // Voice Logs
    private void logVoiceJoin(GuildVoiceUpdateEvent event) {
        Guild guild = event.getGuild();
        Member member = event.getMember();
        String vcId = event.getChannelJoined().getId();
        User user = member.getUser();
        log(guild, embedBuilder -> embedBuilder.setAuthor(getLoggingName(guild, user), null, user.getEffectiveAvatarUrl())
                                               .setColor(Color.BLUE)
                                               .appendDescription(String.format("**%s joined the voice channel <#%s>**", user.getAsMention(), vcId))
                                               .setFooter("User ID: " + user.getId() + " |  VC ID: " + vcId, null)
                                               .setTimestamp(OffsetDateTime.now())
        );
    }

    private void logVoiceMove(GuildVoiceUpdateEvent event) {
        Guild guild = event.getGuild();
        Member member = event.getMember();
        String newVcId = event.getChannelJoined().getId();
        String oldVcId = event.getChannelLeft().getId();
        User user = member.getUser();
        log(guild, embedBuilder -> embedBuilder.setAuthor(getLoggingName(guild, user), null, user.getEffectiveAvatarUrl())
                                               .setColor(Color.BLUE)
                                               .appendDescription(String.format("**%s moved voice channels from <#%s> to <#%s>**", user.getAsMention(), oldVcId, newVcId))
                                               .setFooter(String.format("User ID: %s | Old VC Id: %s | New VC ID: %s", user.getId(), oldVcId, newVcId), null)
                                               .setTimestamp(OffsetDateTime.now())
        );
    }

    private void logVoiceLeave(GuildVoiceUpdateEvent event) {
        Guild guild = event.getGuild();
        Member member = event.getMember();
        String vcId = event.getChannelLeft().getId();
        User user = member.getUser();
        log(guild, embedBuilder -> embedBuilder.setAuthor(getLoggingName(guild, user), null, user.getEffectiveAvatarUrl())
                                               .setColor(Color.BLUE)
                                               .appendDescription(String.format("**%s left the voice channel <#%s>**", user.getAsMention(), vcId))
                                               .setFooter("User ID: " + user.getId() + " |  VC ID: " + vcId, null)
                                               .setTimestamp(OffsetDateTime.now())
        );
    }

    // Avatar Logs
    // TODO: Make look pretty
    public void logAvatarChange(UserUpdateAvatarEvent event) {
        OffsetDateTime now = OffsetDateTime.now();
        List<TextChannel> logs = event.getUser().getMutualGuilds().stream().map(guild -> vortex.getDatabase().settings.getSettings(guild).getAvatarLogChannel(guild)).filter(Objects::nonNull).collect(Collectors.toList());
        if (logs.isEmpty()) {
            return;
        }

        User user = event.getUser();
        vortex.getThreadpool().execute(() -> {
            byte[] im = avatarSaver.makeAvatarImage(event.getUser(), event.getOldAvatarUrl(), event.getOldAvatarId());
            if (im != null) {
                logs.forEach(tc -> {
                    Guild guild = tc.getGuild();
                    try {
                        tc.sendMessage(event.getUser().getAvatarId() != null && event.getUser().getAvatarId().startsWith("a_") ? " <:gif:314068430624129039>" : "")
                          .setFiles(FileUpload.fromData(im, "AvatarChange.png"))
                          .addEmbeds(new EmbedBuilder().setAuthor(getLoggingName(guild, user), null, user.getEffectiveAvatarUrl())
                                                       .setColor(Color.BLUE)
                                                       .setDescription("**" + user.getAsMention() + " changed their avatar**")
                                                       .setFooter("User ID: " + user.getId(), null)
                                                       .setTimestamp(now).build()
                          ).queue();
                    } catch (PermissionException ignore) {}
                });
            }
        });
    }

    public void logModlog(Guild guild, Modlog modlog) {
        String verb;
        boolean punishing = modlog.getSaviorId() == -1;

        verb = switch (modlog.getType()) {
            case GRAVEL -> "graveled";
            case MUTE -> "muted";
            case WARN -> "warned";
            case BAN -> "banned";
            case SOFTBAN -> "softbanned";
            case KICK -> "kicked";
            default -> "";
        };


        String howLong = "";
        Instant finish = modlog.getFinnish(), start = modlog.getStart();
        if (finish != null && start != null && finish.getEpochSecond() != Instant.MAX.getEpochSecond()) {
            howLong = " for " + FormatUtil.secondsToTimeCompact(finish.getEpochSecond() - start.getEpochSecond());
            if (!punishing) {
                howLong = " after they were " + verb + " " + howLong;
            }
        }


        User user = OtherUtil.getUserCacheElseRetrieve(guild.getJDA(), modlog.getUserId());
        final String HOW_LONG = howLong;

        EmbedBuilder builder = new EmbedBuilder();

        if (user != null) {
            builder.setAuthor(getLoggingName(guild, user), null, user.getEffectiveAvatarUrl());
        }

        builder.appendDescription(String.format("<@%d> %s <@%d>%s", punishing ? modlog.getModId() : modlog.getSaviorId(), punishing ? verb : "un" + verb, modlog.getUserId(), HOW_LONG))
               .addField("Case ID", "" + modlog.getId(), true)
               .setTimestamp(punishing ? modlog.getStart() : modlog.getFinnish())
               .setFooter(String.format("User ID: %d | %s ID: %d", modlog.getUserId(), punishing ? "Punisher" : "Pardoner", punishing ? modlog.getModId() : modlog.getSaviorId()), null);

        String reason = modlog.getReason();
        if (reason != null && !reason.isBlank()) {
            builder.addField("Reason", FormatUtil.clamp(reason, MessageEmbed.VALUE_MAX_LENGTH), true);
        }
    }

    public void logModlogUpdate(Guild guild, long caseId, User updatingModerator, String oldReason, String newReason, Temporal now) {
        Function<String, String> formatReason = r -> r == null || r.isBlank() ? "_No Reason Specified_" : FormatUtil.clamp(r, MessageEmbed.VALUE_MAX_LENGTH);

        log(guild, embedBuilder -> embedBuilder.setAuthor(getLoggingName(guild, updatingModerator), null, updatingModerator.getEffectiveAvatarUrl())
                                               .setColor(Color.ORANGE.darker())
                                               .appendDescription(updatingModerator.getAsMention() + " updated the reason for case " + caseId)
                                               .addField("Old Reason", formatReason.apply(oldReason), false)
                                               .addField("New Reason", formatReason.apply(newReason), false)
                                               .setFooter("Updating Moderator ID: " + updatingModerator.getId(), null)
                                               .setTimestamp(now)
        );
    }

    public void logModlogDeletion(Guild guild, Modlog modlog, User deletingModerator) {
        log(guild, embedBuilder -> embedBuilder.setAuthor(getLoggingName(guild, deletingModerator), null, deletingModerator.getEffectiveAvatarUrl())
                                               .setColor(Color.ORANGE.darker())
                                               .appendDescription(deletingModerator.getAsMention() + " deleted a modlog")
                                               .addField("Case " + modlog.getId(), FormatUtil.formatModlogCase(vortex, guild, modlog), true)
                                               .setFooter(String.format("Deleting Moderator ID: %s | User ID: %d | Punisher ID:%d%s", deletingModerator.getId(), modlog.getUserId(), modlog.getModId(), modlog.getSaviorId() == -1 ? "" : " | Pardoner ID: " + modlog.getSaviorId()), null).setTimestamp(Instant.now()));
    }

    /**
     * Purely visual, should be called after doing important audit log logic
     * @param entry The entry to log in a servers modlogs channel
     */
    // TODO: Refactor
    public void logAuditLogEntry(AuditLogEntry entry) {
        Guild guild = entry.getGuild();
        TextChannel tc = vortex.getDatabase().settings.getSettings(guild).getModLogChannel(guild);
        if (tc == null) {
            return;
        }

        Function<EmbedBuilder, EmbedBuilder> builder = null;
        switch (entry.getType()) {
            case MEMBER_ROLE_UPDATE -> {
                String field;
                List<Long> addedRoles = AuditLogReader.getPartialRoles(entry, AuditLogKey.MEMBER_ROLES_ADD);
                List<Long> removedRoles = AuditLogReader.getPartialRoles(entry, AuditLogKey.MEMBER_ROLES_REMOVE);

                String targetMention = "<@" + entry.getTargetId() + ">";
                String modMention = entry.getUserId() == null ? "an unknown moderator" : "<@" + entry.getUserId() + ">";

                if (removedRoles.isEmpty() && !addedRoles.isEmpty()) {
                    field = targetMention + " was given " + FormatUtil.toMentionableRoles(addedRoles) + " by " + modMention;
                } else if (!removedRoles.isEmpty() && addedRoles.isEmpty()) {
                    field = targetMention + " had " + FormatUtil.toMentionableRoles(removedRoles) + " removed by " + modMention;
                } else if (!removedRoles.isEmpty() && !addedRoles.isEmpty()) {
                    field = String.format("%s was given %s and had %s removed by %s", targetMention, FormatUtil.toMentionableRoles(addedRoles), FormatUtil.toMentionableRoles(removedRoles), modMention);
                } else {
                    return;
                }

                builder = embedBuilder -> embedBuilder.setColor(Color.BLUE).setAuthor(getLoggingName(entry), null, getTargetProfilePictureURL(entry)).addField("", field, true).setFooter("User ID: " + entry.getTargetId() + (entry.getUser() == null ? "" : " | Mod ID: " + entry.getUser().getId()), null).setTimestamp(entry.getTimeCreated());
            }
        }

        log(guild, builder);
    }

    public String getLoggingName(AuditLogEntry ale) {
        User u = ale.getJDA().getUserById(ale.getTargetIdLong());
        String nickname = ale.getGuild().getMember(u).getEffectiveName();
        return u.getName() + "#" + u.getDiscriminator() + (nickname != null && !nickname.equals(u.getName()) ? " (" + nickname + ")" : "");
    }

    public String getLoggingName(Guild guild, User u) {
        if (u == null) {
            return "An Unknown User";
        }

        Member m = guild.getMember(u);
        String nickname = m == null ? null : m.getNickname();
        boolean hasUniqueNickname = nickname != null && !nickname.equals(u.getName());
        return FormatUtil.formatUser(u) + (hasUniqueNickname ? " (" + nickname + ")" : "");
    }

    public String getLoggingName(CachedMessage m) {
        return FormatUtil.formatUser(m.getUsername(), m.getDiscriminator());
    }

    public String getTargetProfilePictureURL(AuditLogEntry ale) {
        return ale.getJDA().getUserById(ale.getTargetIdLong()).getEffectiveAvatarUrl();
    }

    public void log(Guild guild, Function<EmbedBuilder, EmbedBuilder> builderFunction) {
        TextChannel tc = vortex.getDatabase().settings.getSettings(guild).getModLogChannel(guild);
        if (tc == null || builderFunction == null) {
            return;
        }

        try {
            tc.sendMessageEmbeds(builderFunction.apply(new EmbedBuilder()).build()).queue();
        } catch (PermissionException ignore) {}
    }

    // TODO: Make is so that you don't have to recompile the entire embed everytime you change the author. Then you can remove the @DoNotUseForVerifiedBots tag
    @DoNotUseForVerifiedBots
    public void log(@NotNull TextChannel modlogsChannel, @NotNull ModlogEmbed modlogEmbed) {
        ModlogEmbedImpl modlogEmbedImpl = (ModlogEmbedImpl) modlogEmbed;

        try {
            FileUpload fileUpload = modlogEmbedImpl.getFileUpload();
            if (fileUpload != null) {
                modlogsChannel.sendFiles(fileUpload).addEmbeds(modlogEmbedImpl.build(modlogsChannel.getGuild())).queue();
            } else {
                modlogsChannel.sendMessageEmbeds(modlogEmbedImpl.build(modlogsChannel.getGuild())).queue();
            }
        } catch (PermissionException ignore) {}
    }

    public void log(Guild guild, EmbedBuilder embedBuilder) {
        TextChannel tc = vortex.getDatabase().settings.getSettings(guild).getModLogChannel(guild);
        if (tc == null || embedBuilder == null) {
            return;
        }

        try {
            tc.sendMessageEmbeds(embedBuilder.build()).queue();
        } catch (PermissionException ignore) {}
    }

    public void logToMutualGuilds(User user, ModlogEmbed modlogEmbed) {
        user.getMutualGuilds()
                .stream()
                .map(this::getModlogsChannel)
                .filter(Objects::nonNull)
                .forEach(modlogsChannel -> log(modlogsChannel, modlogEmbed));
    }

    public TextChannel getModlogsChannel(Guild g) {
        TextChannel channel =  vortex.getDatabase().settings.getSettings(g).getModLogChannel(g);
        return channel == null || !channel.canTalk() ? null : channel;
    }
}