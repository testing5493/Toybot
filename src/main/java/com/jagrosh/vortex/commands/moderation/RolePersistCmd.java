package com.jagrosh.vortex.commands.moderation;

import com.jagrosh.vortex.Action;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.commands.CommandExceptionListener;
import com.jagrosh.vortex.commands.HybridEvent;
import com.jagrosh.vortex.database.managers.GuildSettingsDataManager;
import com.jagrosh.vortex.hibernate.api.ModlogManager;
import com.jagrosh.vortex.utils.FormatUtil;
import com.jagrosh.vortex.utils.LogUtil;
import com.jagrosh.vortex.utils.OtherUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

// TODO: Abstract this so sealing is unneccessary
public abstract sealed class RolePersistCmd extends PunishmentCmd permits GravelCmd, MuteCmd {
    public RolePersistCmd(Vortex vortex, Action action) {
        super(vortex, action, true, Permission.MANAGE_ROLES);
        this.botPermissions = new Permission[]{Permission.MANAGE_ROLES};
    }

    @Override
    protected final void execute(HybridEvent event, long targetId, int min, String reason) {
        boolean isGravel = action == Action.GRAVEL;

        Member mod = event.getMember();
        Guild g = event.getGuild();
        Member targetMember = OtherUtil.getMemberCacheElseRetrieve(g, targetId);
        String userMention = FormatUtil.formatUserMention(targetId);
        GuildSettingsDataManager.GuildSettings guildSettings = vortex.getDatabase().settings.getSettings(g);
        Role persistRole = isGravel ? guildSettings.getGravelRole(g) : guildSettings.getMutedRole(g);

        if (persistRole == null) {
            throw new CommandExceptionListener.CommandErrorException(String.format("No %s role exists!", isGravel ? "Graveled" : "Muted"));
        }

        if (!g.getSelfMember().hasPermission(Permission.MANAGE_ROLES) || !g.getSelfMember().canInteract(persistRole)) {
            throw new CommandExceptionListener.CommandErrorException(String.format("I do not have permission to assign the %s role to users.", isGravel ? "graveled" : "muted"));
        }

        if (targetMember != null) {
            if (!g.getSelfMember().canInteract(targetMember)) {
                throw new CommandExceptionListener.CommandErrorException(String.format("I am unable to %s %s", isGravel ? "gravel" : "mute", userMention));
            } else if (targetMember.getRoles().contains(persistRole)) {
                throw new CommandExceptionListener.CommandErrorException(String.format("%s is already %s!", userMention, isGravel ? "graveled" : "muted"));
            }
        }

        Instant finishTime = min == -1 ? Instant.MAX : Instant.now().plus(min, ChronoUnit.MINUTES);

        String message;
        if (isGravel) {
            String[] messages = {
                    userMention + " was banished to the gravel pit",
                    userMention + " was graveled",
                    userMention + " was sent to find flint",
                    "Added gravel to " + userMention,
                    userMention + " fell in a pit of gravel",
                    " Successfully poured some gravel on " + userMention
            };
            message = messages[(int) (messages.length * Math.random())];
        } else {
            message = userMention + " was muted";
        }

        if (targetMember == null) {
            // TODO: Account for member already being graveled
            logPersist(isGravel, g, targetId, mod.getIdLong(), finishTime, reason);
            event.reply(message);
        } else {
            g.addRoleToMember(targetMember, persistRole).reason(LogUtil.auditReasonFormat(mod, min, reason)).queue(success -> {
                event.reply(message);
                logPersist(isGravel, g, targetId, mod.getIdLong(), finishTime, reason);
            }, failure -> {
                handleError(event, failure, action, targetId);
            });
        }
    }

    private void logPersist(boolean isGravel, Guild g, long targetId, long modId, Instant finishTime, String reason) {
        ModlogManager modlogManager = vortex.getHibernate().modlogs;

        if (isGravel) {
            modlogManager.logGravel(g.getIdLong(), targetId, modId, Instant.now().getEpochSecond(), finishTime.getEpochSecond(), reason);
        } else {
            modlogManager.logMute(g.getIdLong(), targetId, modId, Instant.now().getEpochSecond(), finishTime.getEpochSecond(), reason);
        }
    }
}
