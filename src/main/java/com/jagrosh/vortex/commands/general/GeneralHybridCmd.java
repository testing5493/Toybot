package com.jagrosh.vortex.commands.general;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.commands.CommandTools;
import com.jagrosh.vortex.commands.HybridEvent;
import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.Permission;

/**
 * <p>A simple class that makes writing Hybrid Commands (commands that have a regular and slash interface)
 * more convenient , as you only need to implement {@link GeneralHybridCmd#execute(HybridEvent)} opposed to
 * {@link SlashCommand#execute(CommandEvent)} and {@link SlashCommand#execute(SlashCommandEvent)}</p>
 * <br>
 * Note that {@link GeneralHybridCmd#execute(HybridEvent)} pre checks that the user executing the command has
 * general command perms for you!
 */
@AllArgsConstructor
public abstract class GeneralHybridCmd extends SlashCommand {
    protected final Vortex vortex;

    @Override
    protected final void execute(CommandEvent e) {
        if (!CommandTools.hasGeneralCommandPerms(vortex, e, Permission.MESSAGE_MANAGE)) {
            return;
        }

        execute(HybridEvent.of(e));
    }

    @Override
    protected final void execute(SlashCommandEvent e) {
        if (!CommandTools.hasGeneralCommandPerms(vortex, e, Permission.MESSAGE_MANAGE)) {
            e.reply(CommandTools.COMMAND_NO_PERMS).setEphemeral(true).queue();
            return;
        }

        execute(HybridEvent.of(e));
    }

    protected abstract void execute(HybridEvent e);
}
