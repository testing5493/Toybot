package com.jagrosh.vortex.commands.moderation;

import com.jagrosh.vortex.Action;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.commands.HybridEvent;
import com.jagrosh.vortex.utils.FormatUtil;
import net.dv8tion.jda.api.Permission;

public class WarnCmd extends PunishmentCmd {
    public WarnCmd(Vortex vortex) {
        super(vortex, Action.WARN, false, Permission.ADMINISTRATOR);
        this.name = "warn";
        this.aliases = new String[]{"warning"};
        this.help = "warns users";
        this.guildOnly = true;
    }

    @Override
    protected void execute(HybridEvent event, long userId, int time, String reason) {
        vortex.getDatabase().warnings.logCase(vortex, event.getGuild(), event.getUser().getIdLong(), userId, reason);
        event.reply(FormatUtil.formatUserMention(userId) + " was warned");
    }
}