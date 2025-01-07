package com.jagrosh.vortex.commands.moderation.punish;

import com.jagrosh.vortex.Action;
import com.jagrosh.vortex.Vortex;

public non-sealed class GravelCmd extends RolePersistCmd {
    public GravelCmd(Vortex vortex) {
        super(vortex, Action.GRAVEL);
        this.name = "gravel";
        this.help = "applies graveled role to users";
    }
}
