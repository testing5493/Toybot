package com.jagrosh.vortex.commands.moderation;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import com.jagrosh.vortex.Vortex;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.Collections;

public class DelTagCmd extends ModCmd {
    public DelTagCmd(Vortex vortex) {
        super(vortex, Permission.MESSAGE_MANAGE);
        this.name = "deltag";
        this.arguments = "<tagName>";
        this.help = "deletes a tag";
        this.guildOnly = true;
        this.options = Collections.singletonList(new OptionData(OptionType.STRING, "name", "The tag to delete", true));
    }


    @Override
    protected void execute1(SlashCommandEvent event) {
        String tagName = event.getOption("name", OptionMapping::getAsString);

        if (tagName.isBlank()) {
            event.reply("Please enter a tag to delete").setEphemeral(true).queue();
        }

        if (vortex.getHibernate().tags.delete(event.getGuild().getIdLong(), tagName) != null) {
            event.reply("Successfully deleted the `" + tagName + "` tag").queue();
        } else {
            event.reply("Oops! The tag `" + tagName + "` could not be found.").setEphemeral(true).queue();
        }
    }

    @Override
    public void execute(CommandEvent event) {
        String tagName;
        try {
            tagName = event.getArgs().trim().toLowerCase().split(" ")[0];
        } catch (IndexOutOfBoundsException e) {
            event.reply("Please enter a tag to delete");
            return;
        }

        if (vortex.getHibernate().tags.delete(event.getGuild().getIdLong(), tagName) != null) {
            event.reply("Successfully deleted the `" + tagName + "` tag");
        } else {
            event.reply("Oops! The tag `" + tagName + "` could not be found.");
        }
    }
}