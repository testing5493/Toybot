package com.jagrosh.vortex.commands.general;

import com.jagrosh.vortex.Emoji;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.commands.HybridEvent;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;

@Slf4j
public class MemberCountCmd extends GeneralHybridCmd {
    public MemberCountCmd(Vortex vortex) {
        super(vortex);
        this.guildOnly = true;
        this.name = "membercount";
        this.help = "Shows the amount of members in this server";
        this.aliases = new String[]{"count"};
    }

    protected void execute(HybridEvent e) {
        Guild.MetaData metaData = e.getGuild().retrieveMetaData().complete();
        e.reply(String.format("%s (%s %s)", metaData.getApproximateMembers(), Emoji.STATUS_ONLINE, metaData.getApproximatePresences()));
    }
}
