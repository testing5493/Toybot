/*
 * Copyright 2018 John Grosh (john.a.grosh@gmail.com).
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
package com.jagrosh.vortex;

import com.jagrosh.vortex.logging.MessageCache.CachedMessage;
import net.dv8tion.jda.api.JDA.ShardInfo;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.channel.update.ChannelUpdateSlowmodeEvent;
import net.dv8tion.jda.api.events.guild.GuildBanEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildUnbanEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.message.MessageBulkDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateAvatarEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateDiscriminatorEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateNameEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class Listener implements EventListener {
    private final static Logger LOG = LoggerFactory.getLogger("Listener");
    private final Vortex vortex;

    public Listener(Vortex vortex) {
        this.vortex = vortex;
    }

    @Override
    public void onEvent(@NotNull GenericEvent event) {
        switch (event) {
            case MessageReceivedEvent mre -> {
                Message m = mre.getMessage();
                if (!m.getAuthor().isBot() && m.isFromGuild()) // ignore bot messages
                {
                    // Store the message
                    vortex.getMessageCache().putMessage(m);

                    // Run automod on the message
                    vortex.getAutoMod().performAutomod(m);
                }
            } case MessageUpdateEvent mue -> {
                Message m = mue.getMessage();
                if (!m.getAuthor().isBot() && m.isFromGuild()) // ignore bot edits
                {
                    // Run automod on the message
                    vortex.getAutoMod().performAutomod(m);

                    // Store and log the edit
                    CachedMessage old = vortex.getMessageCache().putMessage(m);
                    vortex.getBasicLogger().logMessageEdit(m, old);
                }
            } case MessageDeleteEvent mevent -> {
                if (mevent.isFromGuild()) {
                    // Log the deletion
                    CachedMessage old = vortex.getMessageCache().pullMessage(mevent.getGuild(), mevent.getMessageIdLong());
                    vortex.getModLogger().setNeedUpdate(mevent.getGuild());
                    vortex.getBasicLogger().logMessageDelete(old);
                }
            }
            case MessageBulkDeleteEvent gevent -> {
                // Get the messages we had cached
                List<CachedMessage> logged = gevent.getMessageIds().stream().map(id -> vortex.getMessageCache().pullMessage(gevent.getGuild(), Long.parseLong(id))).filter(Objects::nonNull).collect(Collectors.toList());

                // Log the deletion
                vortex.getBasicLogger().logMessageBulkDelete(logged, gevent.getMessageIds().size(), gevent.getChannel().asTextChannel());
            }
            case GuildMemberJoinEvent gevent -> {
                OffsetDateTime now = OffsetDateTime.now();

                // Log the join
                vortex.getBasicLogger().logGuildJoin(gevent, now);

                // Perform automod on the newly-joined member
                vortex.getAutoMod().memberJoin(gevent);
            }
            case GuildMemberRemoveEvent gmre -> {
                // Log the member leaving
                vortex.getBasicLogger().logGuildLeave(gmre);

                // Signal the modlogger because someone might have been kicked
                vortex.getModLogger().setNeedUpdate(gmre.getGuild());
            }
            case GuildBanEvent gbe ->
                // Signal the modlogger because someone was banned
                    vortex.getModLogger().setNeedUpdate(gbe.getGuild());
            case GuildUnbanEvent gue ->
                // Signal the modlogger because someone was unbanned
                    vortex.getModLogger().setNeedUpdate((gue).getGuild());
            case GuildMemberRoleAddEvent gmrae -> vortex.getModLogger().setNeedUpdate(gmrae.getGuild());
            case GuildMemberRoleRemoveEvent gmrre -> vortex.getModLogger().setNeedUpdate(gmrre.getGuild());
            case UserUpdateNameEvent unue -> {
                // Log the name change
                vortex.getBasicLogger().logNameChange(unue);
                unue.getUser().getMutualGuilds().stream().map(g -> g.getMember(unue.getUser())).forEach(m -> vortex.getAutoMod().dehoist(m));
            }
            case UserUpdateDiscriminatorEvent uude -> vortex.getBasicLogger().logNameChange(uude);
            case GuildMemberUpdateNicknameEvent gmune -> vortex.getAutoMod().dehoist(gmune.getMember());
            case UserUpdateAvatarEvent uaue -> {
                // Log the avatar change
                if (!uaue.getUser().isBot()) {
                    vortex.getBasicLogger().logAvatarChange(uaue);
                }
            }
            case GuildVoiceUpdateEvent gevent -> {
                if (!gevent.getMember().getUser().isBot()) // ignore bots
                {
                    vortex.getBasicLogger().logVoiceUpdate(gevent);
                }
            }
            case ChannelUpdateSlowmodeEvent cuse ->
                // TODO: Check if this logic is correct, no funky thread things etc.
                    vortex.getDatabase().tempslowmodes.clearSlowmode(cuse.getChannel().asTextChannel());
            case ReadyEvent readyEvent -> {
                // Log the shard that has finished loading
                ShardInfo si = event.getJDA().getShardInfo();
                String shardinfo = si == null ? "N/A" : (si.getShardId() + 1) + "/" + si.getShardTotal();
                LOG.info("Shard " + shardinfo + " is ready.");

                // TODO: Make sure gravels and mutes are checked from before the bot is on
                vortex.getLogWebhook().send("\uD83C\uDF00 Shard `" + shardinfo + "` has connected. Guilds: `" // 🌀
                        + event.getJDA().getGuildCache().size() + "` Users: `" + event.getJDA().getUserCache().size() + "`");
                vortex.getThreadpool().scheduleWithFixedDelay(() -> vortex.getDatabase().tempbans.checkUnbans(vortex, event.getJDA()), 0, 2, TimeUnit.MINUTES);
                vortex.getThreadpool().scheduleWithFixedDelay(() -> vortex.getDatabase().tempmutes.checkUnmutes(event.getJDA(), vortex.getDatabase().settings), 0, 45, TimeUnit.SECONDS);
                vortex.getThreadpool().scheduleWithFixedDelay(() -> vortex.getDatabase().gravels.checkGravels(event.getJDA(), vortex.getDatabase().settings), 0, 45, TimeUnit.SECONDS);
                vortex.getThreadpool().scheduleWithFixedDelay(() -> vortex.getDatabase().tempslowmodes.checkSlowmode(event.getJDA()), 0, 45, TimeUnit.SECONDS);
            }
            case GuildJoinEvent gje -> vortex.getModLogger().addNewGuild(gje.getGuild());
            default -> {
            }
        }
    }
}
