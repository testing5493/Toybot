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
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.commands.HybridEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.attribute.IGuildChannelContainer;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.cache.SnowflakeCacheView;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.awt.*;

/**
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class AboutCmd extends GeneralHybridCmd {
    public AboutCmd(Vortex vortex) {
        super(vortex);
        this.name = "about";
        this.help = "shows info about the bot";
        this.guildOnly = false;
    }

    @Override
    protected void execute(HybridEvent e) {
        JDA jda = e.getJDA();
        Guild g = e.getGuild();
        CommandClient commandClient = e.getClient();

        ShardManager sm = jda.getShardManager();
        SnowflakeCacheView<User> users = sm == null ? jda.getUserCache() : sm.getUserCache();
        double ping = sm == null ? jda.getGatewayPing() : sm.getAverageGatewayPing();
        IGuildChannelContainer gcc = sm == null ? jda : sm;

        MessageEmbed embed = new EmbedBuilder()
                .setColor(g == null ? Color.GRAY : g.getSelfMember().getColor())
                .setTitle("Hi, I'm Toybot!")
                .setDescription(
                        "I'm a modified version of [Vortex](https://github.com/jagrosh/Vortex) that was customised for this server by <@791520107939102730> with the help of <@372268045927972864>, <@384774787823828995>, <@523655829279342593> and <@105725338541101056>, as well as other contributers who can be found on [GitHub](https://github.com/ya64/Vortex). " +
                                "Type `" + commandClient.getPrefix() + commandClient.getHelpWord() + "` for help and information.\n\n")
                .addField("", users.size() + " Users\n" + Math.round(ping) + "ms Ping", true)
                .addField("", gcc.getTextChannelCache().size() + " Text Channels\n" + gcc.getVoiceChannelCache().size() + " Voice Channels", true)
                .setFooter("Last restart", null)
                .setTimestamp(commandClient.getStartTime())
                .build();

        e.reply(MessageCreateData.fromEmbeds(embed));
    }
}
