/*
 * Copyright 2018 John Grosh (jagrosh).
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
package com.jagrosh.vortex.commands.settings;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.hibernate.entities.GuildData;
import com.jagrosh.vortex.utils.DiscordPallete;
import com.jagrosh.vortex.utils.FormatUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;

import java.awt.*;
/**
 * @author John Grosh (jagrosh)
 */
public class SettingsCmd extends Command {
    private final Vortex vortex;

    public SettingsCmd(Vortex vortex) {
        this.vortex = vortex;
        this.name = "settings";
        this.category = new Category("Settings");
        this.help = "shows current settings";
        this.guildOnly = true;
        this.userPermissions = new Permission[]{Permission.MANAGE_SERVER};
    }

    @Override
    protected void execute(CommandEvent event) {
        Guild g = event.getGuild();
        Color color = event.getSelfMember().getColor();
        GuildData guildData = vortex.getHibernate().guild_data.getGuildData(event.getGuild().getIdLong());;
        TextChannel modlogsChannel = guildData.getModlogsChannel(g);
        Role modRole = guildData.getModRole(g);
        Role adminRole = guildData.getAdminRole(g);
        Role muterole = guildData.getMutedRole(g);
        Role gravelrole = guildData.getAdminRole(g);
        MessageEmbed.Field guildSettingsField = new MessageEmbed.Field("\uD83D\uDCCA Server Settings", "Prefix: `" + guildData.getPrefix() + "`" + "\nMod Role: " + (modRole == null ? "None" : modRole.getAsMention()) + "\nAdmin Role: " + (adminRole == null ? "None" : adminRole.getAsMention()) + "\nMuted Role: " + (muterole == null ? "None" : muterole.getAsMention()) + "\nGravel Role: " + (gravelrole == null ? "None" : gravelrole.getAsMention()) + "\nModlogs Channel: " + (modlogsChannel == null ? "None" : modlogsChannel.getAsMention()), true);


        event.getChannel()
                .sendMessage(
                    new MessageCreateBuilder()
                    .setContent(FormatUtil.filterEveryone("**" + event.getSelfUser().getName() + "** settings on **" + event.getGuild().getName() + "**:"))
                    .addEmbeds(
                        new EmbedBuilder()
                        //.setThumbnail(event.getGuild().getIconId()==null ? event.getSelfUser().getEffectiveAvatarUrl() : event.getGuild().getIconUrl())
                        .addField(guildSettingsField)
                        .addField(vortex.getDatabase().automod.getSettingsDisplay(event.getGuild()))
                        .addField(vortex.getDatabase().filters.getFiltersDisplay(event.getGuild()))
                        .setColor(color == null ? DiscordPallete.DEFAULT_ROLE_WHITE : color)
                        .build()
                    ).build()
                ).queue();
    }

}
