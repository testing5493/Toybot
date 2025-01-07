package com.jagrosh.vortex.commands.moderation.pardon;

import com.jagrosh.vortex.Action;
import com.jagrosh.vortex.Vortex;


public non-sealed class UngravelCmd extends RolePersistPardonCmd {
    public UngravelCmd(Vortex vortex) {
        super(vortex, Action.UNGRAVEL);
        this.name = "ungravel";
        this.help = "removes graveled role from users";
    }
}