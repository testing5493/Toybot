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
import com.jagrosh.vortex.Constants;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.hibernate.api.GuildDataManager;
import com.jagrosh.vortex.hibernate.entities.GuildData;
import jakarta.persistence.PersistenceException;
import net.dv8tion.jda.api.Permission;

/**
 * @author John Grosh (john.a.grosh@gmail.com)
 */
// FIXME
public class PrefixCmd extends Command {
    private final Vortex vortex;

    public PrefixCmd(Vortex vortex) {
        this.vortex = vortex;
        this.name = "prefix";
        this.help = "sets the server prefix";
        this.arguments = "<prefix or NONE>";
        this.category = new Category("Settings");
        this.guildOnly = true;
        this.userPermissions = new Permission[]{Permission.MANAGE_SERVER};
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getArgs().isEmpty()) {
            event.replyError("Please include a prefix. The server's current prefix can be seen via the `" + event.getClient().getPrefix() + "settings` command");
            return;
        }

        if (event.getArgs().equalsIgnoreCase("none")) {
            if (setPrefix(event, Constants.PREFIX)) {
                event.replySuccess("The server prefix has been reset.");
            }
            return;
        }

        if (event.getArgs().length() > GuildData.PREFIX_MAX_LENGTH) {
            event.replyError("Prefixes cannot be longer than `" + GuildData.PREFIX_MAX_LENGTH + "` characters.");
            return;
        }

        if (setPrefix(event, event.getArgs())) {
            event.replySuccess("The server prefix has been set to `" + event.getArgs() + "`\n" + "Note that the default prefix (`" + event.getClient().getPrefix() + "`) cannot be removed and will work in addition to the custom prefix.");
        }
    }

    private boolean setPrefix(CommandEvent event, String prefix) {
        try {
            GuildDataManager guildDataManager = vortex.getHibernate().guild_data;
            GuildData guildData = guildDataManager.getGuildData(event.getGuild().getIdLong());
            guildData.setPrefix(prefix);
            guildDataManager.updateGuildData(guildData);
            return true;
        } catch (PersistenceException e) {
            event.replyError("Something went wrong. Please try again later");
            return false;
        }
    }
}
