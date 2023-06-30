package com.jagrosh.vortex.commands.moderation;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import com.jagrosh.vortex.Vortex;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.LinkedList;

public class AddTagCmd extends ModCommand {
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
    protected void execute(SlashCommandEvent event) {
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

        vortex.getDatabase().tags.addTagValue(event.getGuild(), tagName, tagValue);
        event.reply("Successfully created the `" + tagName + "` tag!").setEphemeral(false).queue();
    }

    @Override
    protected void execute(CommandEvent event) {
        String args = event.getArgs().trim();
        String[] argsArray = args.split(" ");
        String tagName;
        String tagValue;

        try {
            tagName = argsArray[0];
        } catch (IndexOutOfBoundsException e) {
            event.reply("Please enter a tag name to create");
            return;
        }

        tagValue = args.substring(argsArray[0].length() + 1);
        if (tagValue.isEmpty()) {
            event.reply("Please enter a value for the tag");
            return;
        }

        vortex.getDatabase().tags.addTagValue(event.getGuild(), tagName, tagValue);
        event.reply("Successfully created the `" + tagName + "` tag!");
    }
}
