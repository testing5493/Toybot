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
import net.dv8tion.jda.api.events.guild.GuildAuditLogEntryCreateEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
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
    public void onEvent(@NotNull GenericEvent genericEvent) {
        switch (genericEvent) {
            case MessageReceivedEvent event -> {
                Message m = event.getMessage();
                if (!m.getAuthor().isBot() && m.isFromGuild()) // ignore bot messages
                {
                    // Store the message
                    vortex.getMessageCache().putMessage(m);

                    // Run automod on the message
                    vortex.getAutoMod().performAutomod(m);
                }
            } case MessageUpdateEvent event -> {
                Message m = event.getMessage();
                if (!m.getAuthor().isBot() && m.isFromGuild()) // ignore bot edits
                {
                    // Run automod on the message
                    vortex.getAutoMod().performAutomod(m);

                    // Store and log the edit
                    CachedMessage old = vortex.getMessageCache().putMessage(m);
                    vortex.getBasicLogger().logMessageEdit(m, old);
                }
            } case MessageDeleteEvent event -> {
                if (event.isFromGuild()) {
                    // Log the deletion
                    CachedMessage old = vortex.getMessageCache().pullMessage(event.getGuild(), event.getMessageIdLong());
                    vortex.getBasicLogger().logMessageDelete(old);
                }
            }
            case MessageBulkDeleteEvent event -> {
                // Get the messages we had cached
                List<CachedMessage> logged = event.getMessageIds().stream().map(id -> vortex.getMessageCache().pullMessage(event.getGuild(), Long.parseLong(id))).filter(Objects::nonNull).collect(Collectors.toList());

                // Log the deletion
                vortex.getBasicLogger().logMessageBulkDelete(logged, event.getMessageIds().size(), event.getChannel().asTextChannel());
            }
            case GuildMemberJoinEvent event -> {
                OffsetDateTime now = OffsetDateTime.now();

                // Log the join
                vortex.getBasicLogger().logGuildJoin(event, now);

                // Perform automod on the newly-joined member
                vortex.getAutoMod().memberJoin(event);
            }
            case GuildMemberRemoveEvent event -> {
                // Log the member leaving
                vortex.getBasicLogger().logGuildLeave(event);
            }
            case UserUpdateNameEvent event -> {
                // Log the name change
                vortex.getBasicLogger().logNameChange(event);
                event.getUser().getMutualGuilds().stream().map(g -> g.getMember(event.getUser())).forEach(m -> vortex.getAutoMod().dehoist(m));
            }
            case UserUpdateDiscriminatorEvent event -> vortex.getBasicLogger().logNameChange(event);
            case GuildMemberUpdateNicknameEvent event -> vortex.getAutoMod().dehoist(event.getMember());
            case UserUpdateAvatarEvent event -> {
                // Log the avatar change
                if (!event.getUser().isBot()) {
                    vortex.getBasicLogger().logAvatarChange(event);
                }
            }
            case GuildVoiceUpdateEvent event -> {
                if (!event.getMember().getUser().isBot()) // ignore bots
                {
                    vortex.getBasicLogger().logVoiceUpdate(event);
                }
            }
            case ChannelUpdateSlowmodeEvent event -> {
                // TODO: Check if this logic is correct, no funky thread things etc.
                vortex.getDatabase().tempslowmodes.clearSlowmode(event.getChannel().asTextChannel());
            }
            case GuildAuditLogEntryCreateEvent event -> {
                vortex.getAuditLogReader().parseEntry(event.getEntry());
            }
            case ReadyEvent event -> {
                // Log the shard that has finished loading
                ShardInfo si = genericEvent.getJDA().getShardInfo();
                String shardinfo = si == null ? "N/A" : (si.getShardId() + 1) + "/" + si.getShardTotal();
                LOG.info("Shard " + shardinfo + " is ready.");

                // TODO: Make sure gravels and mutes are checked from before the bot is on
                vortex.getLogWebhook().send("\uD83C\uDF00 Shard `" + shardinfo + "` has connected. Guilds: `" // 🌀
                        + genericEvent.getJDA().getGuildCache().size() + "` Users: `" + genericEvent.getJDA().getUserCache().size() + "`");
                vortex.getThreadpool().scheduleWithFixedDelay(() -> vortex.getDatabase().tempbans.checkUnbans(vortex, genericEvent.getJDA()), 0, 2, TimeUnit.MINUTES);
                vortex.getThreadpool().scheduleWithFixedDelay(() -> vortex.getDatabase().tempmutes.checkUnmutes(genericEvent.getJDA(), vortex.getDatabase().settings), 0, 45, TimeUnit.SECONDS);
                vortex.getThreadpool().scheduleWithFixedDelay(() -> vortex.getDatabase().gravels.checkGravels(genericEvent.getJDA(), vortex.getDatabase().settings), 0, 45, TimeUnit.SECONDS);
                vortex.getThreadpool().scheduleWithFixedDelay(() -> vortex.getDatabase().tempslowmodes.checkSlowmode(genericEvent.getJDA()), 0, 45, TimeUnit.SECONDS);
                vortex.getThreadpool().schedule(() -> vortex.getAuditLogReader().start(), 0, TimeUnit.SECONDS);
            }
            default -> {}
        }
    }
}
