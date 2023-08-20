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
import com.jagrosh.vortex.commands.CommandExceptionListener.CommandErrorException;
import com.jagrosh.vortex.commands.HybridEvent;
import com.jagrosh.vortex.utils.FormatUtil;
import com.jagrosh.vortex.utils.LogUtil;
import com.jagrosh.vortex.utils.OtherUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

/**
 * @author John Grosh (jagrosh)
 */
public class BanCmd extends PunishmentCmd {
    public BanCmd(Vortex vortex) {
        super(vortex, Action.BAN, true, Permission.BAN_MEMBERS);
        this.name = "ban";
        this.help = "ban user";
        this.botPermissions = new Permission[]{Permission.BAN_MEMBERS};
    }

    @Override
    protected void execute(HybridEvent event, long toBanId, int min, String reason) {
        Guild g = event.getGuild();
        Member mod = event.getMember();
        Member toBanMember = OtherUtil.getMemberCacheElseRetrieve(g, toBanId);

        Role modrole = vortex.getDatabase().settings.getSettings(g).getModeratorRole(g);


        if (!g.getSelfMember().hasPermission(Permission.BAN_MEMBERS)) {
            throw new CommandErrorException("I do not have any roles with permissions to ban users.");
        }

        if (toBanMember != null) {
            if (toBanMember.getUser().isBot()) {
                event.reply("Nice try bitslayn");
            } if (toBanMember.isOwner()) {
                event.reply(FormatUtil.formatUserMention(toBanId) + " was banned");
                return;
            } else if (!mod.canInteract(toBanMember)) {
                throw new CommandErrorException("You do not have permission to ban " + FormatUtil.formatUserMention(toBanId));
            } else if (!g.getSelfMember().canInteract(toBanMember)) {
                throw new CommandErrorException("I am unable to ban " + FormatUtil.formatUserMention(toBanId));
            } else if (modrole != null && toBanMember.getRoles().contains(modrole)) {
                throw new CommandErrorException("I won't ban " + FormatUtil.formatUserMention(toBanId) + " because they are a mod");
            }
        }

        Instant unbanTime = min == -1 ? Instant.MAX : Instant.now().plus(min, ChronoUnit.MINUTES);
        g.ban(UserSnowflake.fromId(toBanId), 0, TimeUnit.SECONDS)
                .reason(LogUtil.auditReasonFormat(mod, min, reason))
                .queue(success -> {
                    vortex.getDatabase().tempbans.setBan(vortex, g, toBanId, mod.getIdLong(), unbanTime, reason);
                    event.reply(FormatUtil.formatUserMention(toBanId) + " was banned");
                }, failure -> {
                    handleError(event, failure, action, toBanId);
                });
    }
}