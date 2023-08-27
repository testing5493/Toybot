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
import com.jagrosh.vortex.utils.FormatUtil;
import com.jagrosh.vortex.utils.OtherUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;

import java.time.Instant;

/**
 * @author John Grosh (jagrosh)
 */
public class UnbanCmd extends PardonCommand {
    public UnbanCmd(Vortex vortex) {
        super(vortex, Action.UNBAN, Permission.BAN_MEMBERS);
        this.name = "unban";
        this.help = "unbans users";
        this.botPermissions = new Permission[]{Permission.BAN_MEMBERS};
    }

    @Override
    protected String execute(Member mod, long targetId) {
        Guild g = mod.getGuild();

        // TODO: Queue this
        try {
            UserSnowflake u = UserSnowflake.fromId(targetId);
            g.retrieveBan(u).complete();
            g.unban(u).complete();
        } catch (ErrorResponseException e) {
            switch (e.getErrorResponse()) {
                case MISSING_PERMISSIONS -> throw new CommandErrorException("I do not have permission to unban users");
                case UNKNOWN_BAN -> {
                    if (OtherUtil.getUserCacheElseRetrieve(g.getJDA(), targetId) == null) {
                        throw new CommandErrorException("The specified user could not be found");
                    } else {
                        throw new CommandErrorException(FormatUtil.formatUserMention(targetId) + " is not banned!");
                    }
                }
                case UNKNOWN_USER -> throw new CommandErrorException("The specified user could not be found");
            }
        } catch (InsufficientPermissionException e) {
            throw new CommandErrorException("I do not have permission to unban users");
        }

        // TODO: Do later???
        vortex.getHibernate().modlogs.logUnban(g.getIdLong(), targetId, mod.getIdLong(), Instant.now());
        return "Unbanned <@" + targetId + ">";
    }
}
