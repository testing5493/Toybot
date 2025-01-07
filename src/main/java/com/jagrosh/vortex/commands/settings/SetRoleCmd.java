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
 * limitations under the License. Furthermore, I'm putting this sentence in all files because I messed up git and its not showing files as edited -\\_( :) )_/-
 */
package com.jagrosh.vortex.commands.settings;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.utils.FinderUtil;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.hibernate.api.GuildDataManager;
import com.jagrosh.vortex.hibernate.entities.GuildData;
import com.jagrosh.vortex.utils.FormatUtil;
import jakarta.persistence.PersistenceException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Role;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class SetRoleCmd extends Command {
    private final Vortex vortex;
    public SetRoleCmd(Vortex vortex) {
        this.vortex = vortex;
        this.name = "setrole";
        this.help = "Sets a server specific role. Valid types include `gravel` `mute` `mod` `admin` `regular` and `member`";
        this.arguments = "<type> <role>";
        this.category = new Category("Settings");
        this.guildOnly = true;
        this.userPermissions = new Permission[]{Permission.MANAGE_SERVER};
    }

    @Override
    protected void execute(CommandEvent event) {
        GuildDataManager guildDataManager = vortex.getHibernate().guild_data;
        GuildData guildData = guildDataManager.getGuildData(event.getGuild().getIdLong());

        String[] args = event.getArgs().split(" ", 2);
        if (event.getArgs().isBlank()) {
            event.replyError(getLongHelp(guildData));
            return;
        }

        RoleType roleType = null;
        for (RoleType roleType1 : RoleType.values()) {
            if (roleType1.getName().equalsIgnoreCase(args[0])) {
                roleType = roleType1;
                break;
            }
        }
        if (roleType == null) {
            event.replyError(getLongHelp(guildData));
            return;
        } else if (roleType.isAdminModifableOnly() && !event.getMember().getPermissions().contains(Permission.ADMINISTRATOR)) {
            event.replyError("You need the administrator permission to modify this setting");
            return;
        }

        long roleId = 0L;
        if (!args[1].trim().equalsIgnoreCase("none")) {
            List<Role> roles = FinderUtil.findRoles(args[1], event.getGuild());
            if (roles.isEmpty()) {
                event.replyError("Role could not be found");
                return;
            } else if (roles.size() == 1) {
                roleId = roles.get(0).getIdLong();
            } else {
                event.replyWarning("Found multiple roles:\n" + FormatUtil.listOfRoles(roles, args[1]));
                return;
            }
        }

        try {
            roleType.getRoleSetter().accept(guildData, roleId);
            guildDataManager.updateGuildData(guildData);
            event.replySuccess("Successfully set the " + roleType.getName() + " role!");
            // TODO: Log to modlogs channel
        } catch (PersistenceException e) {
            event.replyError("Something went wrong. Please try again later.");
        }
    }

    private String getLongHelp(GuildData guildData) {
        StringBuilder builder = new StringBuilder("Please include the type of role as well as the role itself in the following format\n")
                .append(guildData.getPrefix())
                .append(name)
                .append(' ')
                .append(arguments)
                .append("\n\nValid Types:\n");

        for (RoleType roleType : RoleType.values()) {
            builder.append('`').append(roleType.getName()).append("`: ").append(roleType.getDescription()).append('\n');
        }
        builder.append("\n`none` may optionally be supplied for the role itself to indicate this server does not have said role");
        return builder.toString();
    }

    @Getter
    @AllArgsConstructor
    private enum RoleType {
        GRAVEL("gravel", "The gravel role", GuildData::setGravelRoleId, false),
        MUTE("mute", "The muted role", GuildData::setMutedRoleId, false),
        MOD("mod", "The mod (helpful) role. Users with this role will be able to use most moderation commands.", GuildData::setModRoleId, false),
        ADMIN("admin", "The admin (Discord Moderator) role. Users with this role will be able to use all moderation commands short of staging a mutiny. This setting may only be set if the invoker has administrator permissions.", GuildData::setAdminRoleId, true),
        REGULAR("regular", "Regular users role. Users with this role will have general command permissions.", GuildData::setRtcRoleId, false),
        MEMBER("member", "Channel member role. Users with this role will have most general command permissions.", GuildData::setChannelMemberRoleId, false);

        private final String name, description;
        private final BiConsumer<GuildData, Long> roleSetter;
        private final boolean adminModifableOnly;
    }


}
