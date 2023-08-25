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
package com.jagrosh.vortex.commands.moderation;

import com.jagrosh.vortex.Action;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.commands.CommandExceptionListener;
import com.jagrosh.vortex.commands.HybridEvent;
import com.jagrosh.vortex.hibernate.api.ModlogManager;
import com.jagrosh.vortex.utils.FormatUtil;
import com.jagrosh.vortex.utils.LogUtil;
import com.jagrosh.vortex.utils.OtherUtil;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * @author John Grosh (jagrosh)
 */
@Slf4j
public class SoftbanCmd extends PunishmentCmd {
    public SoftbanCmd(Vortex vortex) {
        super(vortex, Action.SOFTBAN, false, Permission.BAN_MEMBERS);
        this.name = "softban";
        this.help = "bans and unbans users";
    }

    @Override
    protected void execute(HybridEvent event, long toBanId, int min, String reason) {
        Member mod = event.getMember();

        Guild g = mod.getGuild();
        Role modrole = vortex.getDatabase().settings.getSettings(g).getModeratorRole(g);
        Member toBanMember = OtherUtil.getMemberCacheElseRetrieve(g, toBanId);

        if (!g.getSelfMember().hasPermission(Permission.BAN_MEMBERS)) {
            throw new CommandExceptionListener.CommandErrorException("I do not have any roles with permissions to ban users.");
        }

        if (toBanMember != null) {
            if (toBanMember.getUser().isBot()) {
                throw new CommandExceptionListener.CommandErrorException("Nice try bitslayn");
            } else if (toBanMember.isOwner()) {
                event.reply(FormatUtil.formatUserMention(toBanId) + " was softbanned");
                return;
            } else if (!mod.canInteract(toBanMember)) {
                throw new CommandExceptionListener.CommandErrorException("You do not have permission to ban " + FormatUtil.formatUserMention(toBanId));
            } else if (!g.getSelfMember().canInteract(toBanMember)) {
                throw new CommandExceptionListener.CommandErrorException("I am unable to ban " + FormatUtil.formatUserMention(toBanId));
            } else if (modrole != null && toBanMember.getRoles().contains(modrole)) {
                throw new CommandExceptionListener.CommandErrorException("I won't ban " + FormatUtil.formatUserMention(toBanId) + " because they are a mod");
            }
        }

        g.ban(UserSnowflake.fromId(toBanId), 0, TimeUnit.SECONDS)
                .reason(LogUtil.auditReasonFormat(mod, min, reason))
                .queue(success -> {
                    g.unban(User.fromId(toBanId))
                            .reason(LogUtil.auditReasonFormat(mod, "Softban Unban"))
                            .queueAfter(4, TimeUnit.SECONDS, success2 -> {
                                vortex.getHibernate().modlogs.logSoftban(g.getIdLong(), toBanId, mod.getIdLong(), Instant.now().getEpochSecond(), reason);
                                event.reply(FormatUtil.formatUserMention(toBanId) + " was softbanned");
                            }, failure2 -> {
                                // If failed to unban
                                vortex.getHibernate().modlogs.logBan(g.getIdLong(), toBanId, mod.getIdLong(), Instant.now().getEpochSecond(), ModlogManager.INDEFINITE_TIME, reason);
                                event.replyError("Failed to unban " + FormatUtil.formatUserMention(toBanId) + " after banning");
                                log.warn("Failed to unban a user after a softban", failure2);
                            });

                }, failure -> {
                    log.warn("Failed to ban a user", failure);
                    event.replyError("Failed to ban " + FormatUtil.formatUserMention(toBanId));
                });
    }
}
