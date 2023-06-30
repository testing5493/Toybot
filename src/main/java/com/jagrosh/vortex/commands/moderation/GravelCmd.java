package com.jagrosh.vortex.commands.moderation;

import com.jagrosh.vortex.Action;
import com.jagrosh.vortex.Vortex;
import lombok.extern.java.Log;
@Log
public non-sealed class GravelCmd extends RolePersistCmd {
    public GravelCmd(Vortex vortex) {
        super(vortex, Action.GRAVEL);
        this.name = "gravel";
        this.help = "applies graveled role to users";
    }
}
