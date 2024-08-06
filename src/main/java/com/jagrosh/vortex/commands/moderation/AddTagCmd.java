package com.jagrosh.vortex.commands.moderation;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import com.jagrosh.vortex.Vortex;
import jakarta.persistence.PersistenceException;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.LinkedList;

public class AddTagCmd extends ModCmd {
    public AddTagCmd(Vortex vortex) {
        super(vortex, Permission.MESSAGE_MANAGE);
        this.name = "addtag";
        this.aliases = new String[]{"edittag"};
        this.arguments = "<tagName> <tagValue>";
        this.help = "adds a tag";
        this.guildOnly = true;
        this.options = new LinkedList<>() {{
           add(new OptionData(OptionType.STRING, "name", "The name of the tag", true));
           add(new OptionData(OptionType.STRING, "value", "The value of the tag", true));
        }};
    }

    @Override
    protected void execute1(SlashCommandEvent event) {
        String tagName = event.getOption("name", OptionMapping::getAsString);
        String tagValue = event.getOption("value", OptionMapping::getAsString);

        if (tagName.isBlank()) {
            event.reply("Please enter a tag name to create").setEphemeral(true).queue();
            return;
        }

        if (tagValue.isBlank()) {
            event.reply("Please enter a value for the tag").setEphemeral(true).queue();
            return;
        }

        try {
            vortex.getHibernate().tags.update(event.getGuild().getIdLong(), tagName, tagValue);
            event.reply("Successfully created the `" + tagName + "` tag!").setEphemeral(false).queue();
        } catch (PersistenceException e) {
            event.reply("An error occurred updating the tags for this server. Please try again.").setEphemeral(true).queue();
        }
    }

    @Override
    protected void execute(CommandEvent event) {
        String args = event.getArgs().trim();
        String[] argsArray = args.split(" ");
        String tagName;
        String tagValue;

        try {
            tagName = argsArray[0];
            if (tagName.isBlank()) {
                event.reply("Please enter a tag name to create");
            }
        } catch (IndexOutOfBoundsException e) {
            event.reply("Please enter a tag name to create");
            return;
        }

        try {
            tagValue = args.substring(argsArray[0].length() + 1);
        } catch (IndexOutOfBoundsException e) {
                event.reply("Please enter a value for the tag");
                return;
        }
        if (tagValue.isEmpty()) {
            event.reply("Please enter a value for the tag");
            return;
        }

        try {
            vortex.getHibernate().tags.update(event.getGuild().getIdLong(), tagName, tagValue.toLowerCase());
            event.reply("Successfully created the `" + tagName + "` tag!");
        } catch (PersistenceException e) {
            event.reply("An error occurred updating the tags for this server. Please try again.");
        }
    }
}
