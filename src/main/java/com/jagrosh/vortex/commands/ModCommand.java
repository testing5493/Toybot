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
package com.jagrosh.vortex.commands;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.vortex.Constants;
import com.jagrosh.vortex.Vortex;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.emoji.Emoji;

/**
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public abstract class ModCommand extends Command {
    protected final Vortex vortex;

    public ModCommand(Vortex vortex, Permission... altPerms) {
        this.vortex = vortex;
        this.guildOnly = true;
        this.category = new Category("Moderation", event -> {
            if (!event.getChannelType().isGuild()) {
                event.replyError("This command is not available in Direct Messages!");
                return false;
            }

            Role modrole = vortex.getDatabase().settings.getSettings(event.getGuild()).getModeratorRole(event.getGuild());
            if (modrole != null && event.getMember().getRoles().contains(modrole)) {
                return true;
            }

            for (Permission altPerm : altPerms) {
                if (altPerm.isChannel()) {
                    if (event.getMember().hasPermission(event.getGuildChannel(), altPerm)) {
                        return true;
                    }
                } else {
                    if (event.getMember().hasPermission(altPerm)) {
                        return true;
                    }
                }
            }

            return false;
        });
    }
}
