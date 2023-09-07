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
import com.jagrosh.vortex.hibernate.api.ModlogManager;
import com.jagrosh.vortex.hibernate.entities.ModLog;
import com.jagrosh.vortex.hibernate.entities.TimedLog;
import com.jagrosh.vortex.logging.MessageCache.CachedMessage;
import com.jagrosh.vortex.utils.*;
import com.typesafe.config.Config;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateAvatarEvent;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.jetbrains.annotations.NotNull;

import java.time.OffsetDateTime;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * @author John Grosh (john.a.grosh@gmail.com)
 */
// TODO: Make sure all values are clamped
// TODO: Check if bot can speak in a modlogs channel before creating the message
@Slf4j
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
        ModlogEmbed modlogEmbed = ModlogEmbed.createForSingleGuild()
                .setTargetUser(author)
                .formatDescription("[Message](%s) sent by %s edited in %s",
                        newMessage.getJumpUrl(),
                        author,
                        newMessage.getChannel()
                ).appendIdToFooter("Message", newMessage.getIdLong())
                .addField("Before", FormatUtil.formatMessage(oldMessage), false) // TODO: Ditch FormatUtil#formatMessage
                .addField("After", FormatUtil.formatMessage(newMessage), false)
                .setColor(DiscordPallete.BLURPLE)
                .setIcon(Emoji.LOGS.EDIT.blurpleIcon(false))
                .appendIdToFooter("Channel",  newMessage.getChannel())
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
                .formatDescription("[Message](%s) sent by <@%d> deleted in <#%d>%n%s", oldMessage.getJumpUrl(), oldMessage.getAuthorId(), oldMessage.getTextChannelId(), formatted)
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
                          .formatDescription("Bulk delete in %s, %d messages were deleted%nClick to [view](%s) or [download](%s) the deleted messages", text, count, view, download)
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
                .formatDescription("%s purged %d message%s from %s%nCriteria: %s%n%sClick to [view](%s) or [download](%s) the deleted message%s.",
                        moderator,
                        count,
                        plural ? "s" : "",
                        target,
                        criteria,
                        reason == null || reason.trim().isEmpty() ? "" : "Reason: " + reason + "\n",
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

        StringBuilder sb = new StringBuilder(REDIR_END + " " + link + "");
        for (int i = 0; i < redirects.size(); i++) {
            sb.append("\n").append(redirects.size() - 1 == i ? REDIR_END + " " : REDIR_MID).append(redirects.get(i)).append(redirects.size() - 1 == i ? "" : "");
        }

        // log(OffsetDateTime.now(), tc, REDIRECT, FormatUtil.formatFullUser(message.getAuthor()) + "'s message in " + message.getChannel().getAsMention() + " contained redirects:", new EmbedBuilder().setColor(Color.BLUE.brighter().brighter()).appendDescription(sb.toString()).build());
         */
    }

    public void logNameChange(User user, String oldUsername, String newUsername) {
        ModlogEmbed modlogEmbed = ModlogEmbed.createForMultiGuild()
                .setTargetUser(user)
                .formatDescription("%s has changed their username from %s to %s", oldUsername, newUsername)
                .setColor(DiscordPallete.BLURPLE)
                .setIcon(Emoji.LOGS.PROFILE_EDIT.blurpleIcon(false));

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
            modlogEmbed.formatDescription("%s joined the server%nThis account has joined \"before\" being created and is definitely an alt.", m)
                    .setColor(DiscordPallete.RED)
                    .setIcon(Emoji.LOGS.JOIN_ALT.yellowIcon(false));
        } else {
            modlogEmbed.formatDescription("%s joined the server%nAccount created on %s (%s)",
                            m,
                            TimeFormat.DATE_TIME_SHORT.format(m.getTimeJoined()),
                            TimeFormat.RELATIVE.format(m.getTimeJoined()))
                    .setColor(DiscordPallete.YELLOW)
                    .setIcon(Emoji.LOGS.JOIN.greenIcon(false));
        }

        log(modlogsChannel, modlogEmbed);
    }

    public void logGuildLeave(GuildMemberRemoveEvent event) {
        TextChannel modlogsChannel = getModlogsChannel(event.getGuild());
        if (modlogsChannel == null) {
            return;
        }

        User u = event.getUser();
        ModlogEmbed modlogEmbed = ModlogEmbed.createForSingleGuild()
                .setTargetUser(event.getMember() == null ? event.getUser() : event.getMember())
                .setColor(DiscordPallete.BLURPLE)
                .setIcon(Emoji.LOGS.LEAVE.redIcon(false))
                .formatDescription("%s left of was kicked/banned from the server", u);

        log(modlogsChannel, modlogEmbed);
    }

    public void logVoiceUpdate(GuildVoiceUpdateEvent event) {
        TextChannel modlogsChannel = getModlogsChannel(event.getGuild());
        if (modlogsChannel == null) {
            return;
        }

        AudioChannelUnion left = event.getChannelLeft();
        AudioChannelUnion joined = event.getChannelJoined();
        Function<AudioChannelUnion, String> getTypeName = channel -> channel.getType() == ChannelType.STAGE ? "Stage" : "VC";
        Member m = event.getMember();
        ModlogEmbed modlogEmbed = ModlogEmbed.createForSingleGuild()
                .setColor(DiscordPallete.BLURPLE)
                .setTargetUser(event.getMember());

        if (joined != null && left != null) {
            modlogEmbed.formatDescription("%s moved from %s to %s", m, left, joined)
                    .appendIdToFooter("Old " + getTypeName.apply(left), left)
                    .appendIdToFooter("New " + getTypeName.apply(joined), joined)
                    .setIcon(Emoji.LOGS.MOVE.blurpleIcon(false));
        } else if (left == null) {
            modlogEmbed.formatDescription("%s joined %s", m, joined)
                    .appendIdToFooter(getTypeName.apply(joined), joined)
                    .setIcon(Emoji.LOGS.JOIN.greenIcon(false));
        } else {
            modlogEmbed.formatDescription("%s left %s", m, left)
                    .appendIdToFooter(getTypeName.apply(left), left)
                    .setIcon(Emoji.LOGS.LEAVE.redIcon(false));
        }

        log(modlogsChannel, modlogEmbed);
    }

    public void logAvatarChange(UserUpdateAvatarEvent event) {
        User user = event.getUser();
        if (user.getMutualGuilds().isEmpty()) {
            return;
        }

        vortex.getThreadpool().execute(() -> {
            byte[] im = avatarSaver.makeAvatarImage(event.getUser(), event.getOldAvatarUrl(), event.getOldAvatarId());
            if (im == null) {
                return;
            }

            ModlogEmbed modlogEmbed = ModlogEmbed.createForMultiGuild()
                    .setColor(DiscordPallete.BLURPLE)
                    .setIcon(Emoji.LOGS.PROFILE_EDIT.blurpleIcon(false))
                    .setTargetUser(user)
                    .setImage(FileUpload.fromData(im, "AvatarChange.png"))
                    .formatDescription("%s changed their profile picture%n[Old PFP](%s), [New PFP](%s)", user, event.getOldAvatarUrl(), event.getNewAvatarUrl());
            logToMutualGuilds(user, modlogEmbed);
        });
    }

    public static final String NO_REASON_PROVIDED = "_No Reason Provided_";

    public void logPunish(Guild guild, ModLog modLog) {
        TextChannel modlogsChannl = getModlogsChannel(guild);
        if (modlogsChannl == null) {
            return;
        }

        UserSnowflake targetUserSnowflake = OtherUtil.getMostRelevent(guild, User.fromId(modLog.getUserId()));
        UserSnowflake modUserSnowflake;
        long punishingModId = modLog.getPunishingModId();
        String modMention;
        if (punishingModId == ModlogManager.NOT_YET_PARDONED_MOD_ID || punishingModId == ModlogManager.UNKNOWN_MOD_ID) {
            modUserSnowflake = null;
            modMention = "an unknown moderator";
        } else if (punishingModId == ModlogManager.AUTOMOD_ID) {
            modUserSnowflake = null;
            modMention = "automod";
        } else {
            modUserSnowflake = OtherUtil.getMostRelevent(guild, User.fromId(punishingModId));
            modMention = modUserSnowflake.getAsMention();
        }

        ModlogEmbed modlogEmbed = ModlogEmbed.createForSingleGuild()
                                             .setColor(DiscordPallete.RED)
                                             .setTargetUser(targetUserSnowflake)
                                             .setModerator(modUserSnowflake)
                                             .setIcon(modLog.actionType().getEmoji().redIcon(false))
                                             .setTimestamp(modLog.getPunishmentTime());

        if (!(modLog instanceof TimedLog timedLog && !timedLog.isPunishedIndefinitely())) {
            modlogEmbed.formatDescription("%s was %s by %s", targetUserSnowflake, modLog.actionType().getPastVerb(), modMention);
        } else {
            long length = timedLog.getPardoningTime().getEpochSecond() - timedLog.getPunishmentTime().getEpochSecond();
            modlogEmbed.formatDescription("%s was %s by %s for %s", targetUserSnowflake, modLog.actionType().getPastVerb(), modMention, FormatUtil.secondsToTimeCompact(length));
        }

        modlogEmbed.addField("Case", "" + modLog.getCaseId(), true)
                .addField(FormatUtil.capitalize(modLog.actionType().getVerb()) + " Reason", !modLog.hasReason() ? NO_REASON_PROVIDED : modLog.getReason());
        log(modlogsChannl, modlogEmbed);
    }

    public void logPardon(Guild guild, TimedLog timedLog) {
        TextChannel modlogsChannl = getModlogsChannel(guild);
        if (modlogsChannl == null || timedLog.getPardoningModId() == ModlogManager.NOT_YET_PARDONED_MOD_ID) {
            return;
        }

        UserSnowflake targetUserSnowflake = OtherUtil.getMostRelevent(guild, User.fromId(timedLog.getUserId()));
        UserSnowflake targetModUserSnowflake;
        String punishingModMention, pardoningModMention;

        if (timedLog.getPunishingModId() == ModlogManager.NOT_YET_PARDONED_MOD_ID || timedLog.getPunishingModId() == ModlogManager.UNKNOWN_MOD_ID) {
            punishingModMention = "an unknown moderator";
        } else if (timedLog.getPunishingModId() == ModlogManager.AUTOMOD_ID) {
            punishingModMention = "_Automod_";
        } else {
            punishingModMention = "<@" + timedLog.getPunishingModId() + ">";
        }

        if (timedLog.getPardoningModId() == ModlogManager.UNKNOWN_MOD_ID) {
            targetModUserSnowflake = null;
            pardoningModMention = "an unknown moderator";
        } else if (timedLog.getPardoningModId() == ModlogManager.AUTOMOD_ID) {
            targetModUserSnowflake = null;
            pardoningModMention = "automod";
        } else {
            targetModUserSnowflake = OtherUtil.getMostRelevent(guild, User.fromId(timedLog.getPardoningModId()));
            pardoningModMention = "<@" + timedLog.getPardoningModId() + ">";
        }

        ModlogEmbed modlogEmbed = ModlogEmbed.createForSingleGuild()
                .setColor(DiscordPallete.RED)
                .setTargetUser(targetUserSnowflake)
                .setModerator(targetModUserSnowflake)
                .setIcon(timedLog.pardonActionType().getEmoji().redIcon(false))
                .setTimestamp(timedLog.getPardoningTime())
                .formatDescription("%s was %s by %s", targetUserSnowflake, timedLog.pardonActionType().getPastVerb(), pardoningModMention)
                .addField("Case", "" + timedLog.getCaseId(), true)
                .addField("Punishing Mod", punishingModMention, true)
                .addField("Time " + FormatUtil.capitalize(timedLog.actionType().getPastVerb()), FormatUtil.formatCreationTime(timedLog.getPunishmentTime()), true)
                .addField(FormatUtil.capitalize(timedLog.actionType().getVerb()) + " Reason", !timedLog.hasReason() ? NO_REASON_PROVIDED : timedLog.getReason());

        log(modlogsChannl, modlogEmbed);
    }

    // TODO: Potentially add more case specific information
    public void logModlogUpdate(Guild guild, long caseId, User updatingModerator, String oldReason, String newReason, Temporal now) {
        TextChannel textChannel = getModlogsChannel(guild);
        if (textChannel == null) {
            return;
        }

        Function<String, String> formatReason = r -> r == null || r.isBlank() ? "_No Reason Specified_" : r;
        ModlogEmbed modlogEmbed = ModlogEmbed.createForSingleGuild()
                .setModerator(updatingModerator)
                .setColor(DiscordPallete.YELLOW)
                .setIcon(Emoji.LOGS.EDIT.yellowIcon(false))
                .formatDescription("%s updated the reason for case %d", updatingModerator, caseId)
                .addField("Old Reason", formatReason.apply(oldReason), false)
                .addField("New Reason", formatReason.apply(newReason), false);

        log(textChannel, modlogEmbed);
    }

    public void logMemberRoleUpdate(Guild g, Long targetId, Long updaterId, @NotNull List<Long> addedRoles, @NotNull List<Long> removedRoles, TemporalAccessor time) {
        TextChannel modlogsChannel = getModlogsChannel(g);
        if (modlogsChannel == null) {
            return;
        }

        ModlogEmbed modlogEmbed = ModlogEmbed.createForSingleGuild()
                .setColor(DiscordPallete.BLURPLE)
                .setIcon(Emoji.LOGS.ROLE.blueIcon(false))
                .setTargetUser(User.fromId(targetId))
                .setTimestamp(time);

        String targetMention = "<@" + targetId + ">";
        String modMention = updaterId == null ? "an unknown moderator" : "<@" + updaterId + ">";
        boolean selfUpdate = Objects.equals(targetId, updaterId);
        if (!selfUpdate && updaterId != null) {
            modlogEmbed.setModerator(User.fromId(updaterId));
        }

        if (removedRoles.isEmpty() && !addedRoles.isEmpty()) {
            if (selfUpdate) {
                modlogEmbed.formatDescription("%s gave %s to themselves", targetMention, FormatUtil.toMentionableRoles(addedRoles));
            } else {
                modlogEmbed.formatDescription("%s was given %s by %s", targetMention, FormatUtil.toMentionableRoles(addedRoles), modMention);
            }
        } else if (!removedRoles.isEmpty() && addedRoles.isEmpty()) {
            if (selfUpdate) {
                modlogEmbed.formatDescription("%s removed %s from themselves", targetMention, FormatUtil.toMentionableRoles(removedRoles));
            } else {
                modlogEmbed.formatDescription("%s had %s removed by %s", targetMention, FormatUtil.toMentionableRoles(removedRoles), modMention);
            }
        } else if (!removedRoles.isEmpty() && !addedRoles.isEmpty()) {
            if (selfUpdate) {
                modlogEmbed.formatDescription("%s gave themselves %s and removed %s", targetMention, FormatUtil.toMentionableRoles(addedRoles), FormatUtil.toMentionableRoles(removedRoles));
            } else {
                modlogEmbed.formatDescription("%s was given %s and had %s removed by %s", targetMention, FormatUtil.toMentionableRoles(addedRoles), FormatUtil.toMentionableRoles(removedRoles), modMention);
            }
        } else {
            return;
        }

        log(modlogsChannel, modlogEmbed);
    }

    // TODO: Make is so that you don't have to recompile the entire embed everytime you change the author. Then you can remove the @DoNotUseForVerifiedBots tag
    @DoNotUseForVerifiedBots
    public void log(@NotNull TextChannel modlogsChannel, @NotNull ModlogEmbed modlogEmbed) {
        ModlogEmbedImpl modlogEmbedImpl = (ModlogEmbedImpl) modlogEmbed;

        FileUpload fileUpload = modlogEmbedImpl.getFileUpload();
        if (fileUpload != null) {
            modlogsChannel.sendFiles(fileUpload).addEmbeds(modlogEmbedImpl.build(modlogsChannel.getGuild())).queue(s -> {}, t -> log.error("Error uploading modlog with file", t));
        } else {
            modlogsChannel.sendMessageEmbeds(modlogEmbedImpl.build(modlogsChannel.getGuild())).queue();
        }
    }

    public void logToMutualGuilds(User user, ModlogEmbed modlogEmbed) {
        user.getMutualGuilds()
                .stream()
                .map(this::getModlogsChannel)
                .filter(Objects::nonNull)
                .forEach(modlogsChannel -> log(modlogsChannel, modlogEmbed));
    }

    public TextChannel getModlogsChannel(Guild g) {
        if (g == null) {
            return null;
        }

        TextChannel channel = vortex.getHibernate().guild_data.getGuildData(g.getIdLong()).getModlogsChannel(g);
        return channel == null || !channel.canTalk() ? null : channel;
    }
}