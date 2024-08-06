package com.jagrosh.vortex.commands.moderation.punish;

import com.jagrosh.vortex.Action;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.commands.HybridEvent;
import com.jagrosh.vortex.utils.FormatUtil;
import net.dv8tion.jda.api.Permission;

import java.time.Instant;
    
public class WarnCmd extends PunishCmd {
    public WarnCmd(Vortex vortex) {
        super(vortex, Action.WARN, false, Permission.ADMINISTRATOR);
        this.name = "warn";
        this.aliases = new String[]{"warning"};
        this.help = "warns users";
        this.guildOnly = true;
    }

    @Override
    protected void execute(HybridEvent event, long userId, int time, String reason) {
        vortex.getHibernate().modlogs.logWarning(event.getGuild().getIdLong(), userId, event.getUser().getIdLong(), Instant.now(), reason);
        event.reply(FormatUtil.formatUserMention(userId) + " was warned");
    }
}