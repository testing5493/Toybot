package com.jagrosh.vortex.commands.moderation;

import com.jagrosh.vortex.Action;
import com.jagrosh.vortex.Vortex;

public non-sealed class MuteCmd extends RolePersistCmd {
    public MuteCmd(Vortex vortex) {
        super(vortex, Action.MUTE);
        this.name = "mute";
        this.help = "applies muted role to users";
    }
}