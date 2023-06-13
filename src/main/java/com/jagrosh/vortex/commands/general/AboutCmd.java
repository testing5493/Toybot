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
package com.jagrosh.vortex.commands.general;

import com.jagrosh.jdautilities.command.CommandClient;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import com.jagrosh.vortex.Constants;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.commands.CommandTools;
import com.jagrosh.vortex.utils.FormatUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.attribute.IGuildChannelContainer;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.cache.SnowflakeCacheView;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.awt.*;

/**
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class AboutCmd extends SlashCommand {
    private final Vortex vortex;

    public AboutCmd(Vortex vortex) {
        this.name = "about";
        this.help = "shows info about the bot";
        this.guildOnly = false;
        this.vortex = vortex;
    }

    @Override
    protected void execute(SlashCommandEvent event) {
        if (!CommandTools.hasGeneralCommandPerms(vortex, event, Permission.MESSAGE_MANAGE)) {
            event.reply(CommandTools.COMMAND_NO_PERMS).setEphemeral(true).queue();
        }

        event.reply(generateReply(event.getJDA(), event.getGuild(), event.getClient())).queue();
    }

    @Override
    protected void execute(CommandEvent event) {
        if (!CommandTools.hasGeneralCommandPerms(vortex, event, Permission.MESSAGE_MANAGE)) {
            return;
        }

        event.reply(generateReply(event.getJDA(), event.getGuild(), event.getClient()));
    }

    private MessageCreateData generateReply(JDA jda, Guild g, CommandClient commandClient) {
        ShardManager sm = jda.getShardManager();
        SnowflakeCacheView<User> users = sm == null ? jda.getUserCache() : sm.getUserCache();
        double ping = sm == null ? jda.getGatewayPing() : sm.getAverageGatewayPing();
        IGuildChannelContainer gcc = sm == null ? jda : sm;

        return MessageCreateData.fromEmbeds(new EmbedBuilder()
                               .setColor(g == null ? Color.GRAY : g.getSelfMember().getColor())
                               .setTitle("Hi, I'm Toybot!")
                               .setDescription(
                                       "I'm a modified version of [Vortex](https://github.com/jagrosh/Vortex) that was customised for this server by <@791520107939102730> with the help of <@372268045927972864>, <@384774787823828995>, <@523655829279342593> and <@105725338541101056>, as well as other contributers who can be found on [GitHub](https://github.com/ya64/Vortex). " +
                                       "Type `" + commandClient.getPrefix() + commandClient.getHelpWord() + "` for help and information.\n\n")
                               .addField("", users.size() + " Users\n" + Math.round(ping) + "ms Ping", true)
                               .addField("", gcc.getTextChannelCache().size() + " Text Channels\n" + gcc.getVoiceChannelCache().size() + " Voice Channels", true)
                               .setFooter("Last restart", null)
                               .setTimestamp(commandClient.getStartTime())
                               .build()
                );
    }
}
