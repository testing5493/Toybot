package com.jagrosh.vortex.commands.moderation;

import com.jagrosh.vortex.Action;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.commands.CommandExceptionListener.CommandErrorException;
import com.jagrosh.vortex.hibernate.api.ModlogManager;
import com.jagrosh.vortex.hibernate.entities.GuildData;
import com.jagrosh.vortex.utils.FormatUtil;
import com.jagrosh.vortex.utils.LogUtil;
import com.jagrosh.vortex.utils.OtherUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;

import java.time.Instant;

// TODO: Abstract this so sealing is unneccessary
public abstract sealed class RolePersistPardonCmd extends PardonCommand permits UngravelCmd, UnmuteCmd {

    public RolePersistPardonCmd(Vortex vortex, Action action) {
        super(vortex, action, Permission.MANAGE_ROLES);
        this.botPermissions = new Permission[]{Permission.MANAGE_ROLES};
    }

    protected final String execute(Member mod, long targetId) {
        boolean isGravel = action == Action.UNGRAVEL;
        Guild g = mod.getGuild();
        Member targetMember = OtherUtil.getMemberCacheElseRetrieve(g, targetId);
        String userMention = FormatUtil.formatUserMention(targetId);
        GuildData guildData = vortex.getHibernate().guild_data.getGuildData(g.getIdLong());
        Role persistRole = isGravel ? guildData.getGravelRole(g) : guildData.getMutedRole(g);

        if (persistRole == null) {
            throw new CommandErrorException(String.format("No %s role exists!", isGravel ? "Graveled" : "Muted"));
        }

        if (!g.getSelfMember().hasPermission(Permission.MANAGE_ROLES) || !g.getSelfMember().canInteract(persistRole)) {
            throw new CommandErrorException(String.format("I do not have permission to take away the %s role from users.", isGravel ? "graveled" : "muted"));
        }

        if (targetMember != null) {
            if (!g.getSelfMember().canInteract(targetMember)) {
                throw new CommandErrorException(String.format("I am unable to %s %s", isGravel ? "ungravel" : "munute", userMention));
            } else if (!targetMember.getRoles().contains(persistRole)) {
                // TODO: Account for graveled in database but doesn't have the gravel role for whatever reason
                throw new CommandErrorException(String.format("%s is not %s!", userMention, isGravel ? "graveled" : "muted"));
            }

            try {
                g.removeRoleFromMember(targetMember, persistRole).reason(LogUtil.auditReasonFormat(mod, isGravel ? "Ungravel" : "Unmute")).complete();
            } catch (ErrorResponseException e) {
                switch (e.getErrorResponse()) {
                    case MISSING_PERMISSIONS -> throw new CommandErrorException("I do not have the sufficient permissions to " + (isGravel ? "ungravel" : "unmute") + userMention);
                    case UNKNOWN_ROLE -> throw new CommandErrorException(String.format("No %s role exists!", isGravel ? "Graveled" : "Muted"));
                }
            }
        }

        // TODO: do async maybe
        logPersistPardon(isGravel, g, targetId, mod.getIdLong());
        return userMention + " was " + (isGravel ? "ungraveled" : "unmuted");
    }

    private void logPersistPardon(boolean isGravel, Guild g, long targetId, long modId) {
        ModlogManager modlogManager = vortex.getHibernate().modlogs;

        if (isGravel) {
            modlogManager.logUngravel(g.getIdLong(), targetId, modId, Instant.now());
        } else {
            modlogManager.logUnmute(g.getIdLong(), targetId, modId, Instant.now());
        }
    }
}
