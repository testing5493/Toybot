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
import com.jagrosh.vortex.utils.OtherUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

/**
 * @author John Grosh (jagrosh)
 */
// TODO: Make this log in the database/modlogs cmd?
public class VoicekickCmd extends PunishmentCmd {
    public VoicekickCmd(Vortex vortex) {
        super(vortex, Action.VOICE_KICK, false, Permission.VOICE_MOVE_OTHERS, Permission.MANAGE_CHANNEL);
        this.name = "voicekick";
        this.aliases = new String[]{"vckick"};
        this.help = "removes users from voice channels";
        this.botPermissions = new Permission[]{Permission.MANAGE_CHANNEL};
    }

    @Override
    protected void execute(HybridEvent e, long userId, int min, String reason) {
        Guild g = e.getGuild();
        Member selfMember = g.getSelfMember();
        if (!selfMember.hasPermission(Permission.VOICE_MOVE_OTHERS, Permission.VOICE_CONNECT)) {
            throw new CommandErrorException("I need permission to connect to voice channels and move members to do that!");
        }

        Role modrole = vortex.getDatabase().settings.getSettings(g).getModeratorRole(g);
        Member targetMember = OtherUtil.getMemberCacheElseRetrieve(g, userId);
        String userMention = FormatUtil.formatUserMention(userId);

        if (targetMember == null) {
            throw new CommandErrorException("I am unable to voicekick " + userMention + " as they are not in this server");
        } else if (!e.getMember().canInteract(targetMember)) {
            throw new CommandErrorException("You do not have permission to voicekick " + userMention);
        } else if (!selfMember.canInteract(targetMember)) {
            throw new CommandErrorException("I am unable to voicekick " + userMention);
        } else if (targetMember.getVoiceState() == null || !targetMember.getVoiceState().inAudioChannel()) {
            throw new CommandErrorException(userMention + " is not in a voice channel!");
        } else if (modrole != null && targetMember.getRoles().contains(modrole)) {
            throw new CommandErrorException(" I won't voicekick " + userMention + " because they are a mod");
        }

        g.kickVoiceMember(targetMember).queue(success -> {
            e.reply(userMention + " was voicekicked");
        }, failure -> {
            handleError(e, failure, action, userId);
        });
    }
}
