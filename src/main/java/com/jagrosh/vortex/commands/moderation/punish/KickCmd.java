/*
 * Copyright 2016 John Grosh (jagrosh).
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
package com.jagrosh.vortex.commands.moderation.punish;

import com.jagrosh.vortex.Action;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.commands.CommandExceptionListener;
import com.jagrosh.vortex.commands.HybridEvent;
import com.jagrosh.vortex.utils.FormatUtil;
import com.jagrosh.vortex.utils.LogUtil;
import com.jagrosh.vortex.utils.OtherUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.UserSnowflake;

import java.time.Instant;

/**
 * @author John Grosh (jagrosh)
 */
public class KickCmd extends PunishCmd {
    public KickCmd(Vortex vortex) {
        super(vortex, Action.KICK, false, Permission.KICK_MEMBERS);
        this.name = "kick";
        this.help = "kicks users";
    }

    @Override
    protected void execute(HybridEvent event, long targetId, int min, String reason) {
        Member mod = event.getMember();

        Guild g = event.getGuild();
        Role modRole = vortex.getHibernate().guild_data.getGuildData(g.getIdLong()).getModRole(g);
        Role adminRole = vortex.getHibernate().guild_data.getGuildData(g.getIdLong()).getAdminRole(g);

        Member targetMember = OtherUtil.getMemberCacheElseRetrieve(g, targetId);
        if (!g.getSelfMember().hasPermission(Permission.KICK_MEMBERS)) {
            throw new CommandExceptionListener.CommandErrorException("I do not have any roles with permissions to kick users.");
        }

        if (targetMember != null) {
            if (targetMember.getUser().isBot()) {
                if (targetMember.getIdLong() == 83010416610906112L) {
                    throw new CommandExceptionListener.CommandErrorException("[Nice try bitslayn](https://discord.com/channels/352726171101954058/352726171101954060/596951253838462976)");
                } else {
                    throw new CommandExceptionListener.CommandErrorException("Nice try bitslayn");
                }
            } else if (!mod.canInteract(targetMember)) {
                throw new CommandExceptionListener.CommandErrorException("You do not have permission to kick " + FormatUtil.formatUserMention(targetId));
            } else if (!g.getSelfMember().canInteract(targetMember)) {
                throw new CommandExceptionListener.CommandErrorException("I am unable to kick " + FormatUtil.formatUserMention(targetId));
            } else if ((modRole != null && targetMember.getRoles().contains(modRole)) || (adminRole != null && targetMember.getRoles().contains(adminRole))) {
                throw new CommandExceptionListener.CommandErrorException("I won't kick " + FormatUtil.formatUserMention(targetId) + " because they are a mod");
            }
        } else {
            throw new CommandExceptionListener.CommandErrorException("I am unable to kick " + FormatUtil.formatUserMention(targetId) + " as they are not in this server");
        }

        g.kick(UserSnowflake.fromId(targetId))
                .reason(LogUtil.auditReasonFormat(mod, reason))
                .queue(success -> {
                    vortex.getHibernate().modlogs.logKick(g.getIdLong(), targetId, mod.getIdLong(), Instant.now(), reason);
                    event.replyError(FormatUtil.formatUserMention(targetId) + " was kicked");
                }, failure -> {
                    handleError(event, failure, action, targetId);
                });
    }
}
