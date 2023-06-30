package com.jagrosh.vortex.commands.general;

import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.commands.HybridEvent;
import com.jagrosh.vortex.utils.OtherUtil;

public class RatCmd extends GeneralHybridCmd {
    public RatCmd(Vortex vortex) {
        super(vortex);
        this.name = "rat";
        this.help = "rat poggers";
        this.guildOnly = false;
        this.aliases = new String[]{"ratImage"};
    }

    @Override
    protected void execute(HybridEvent e) {
        String[] rats = OtherUtil.readLines("rats");
        e.reply(rats[(int) (Math.random() * rats.length)]);
    }
}
