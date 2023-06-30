package com.jagrosh.vortex.commands.general;

import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.commands.HybridEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.Collections;

public class TagCmd extends GeneralHybridCmd {
    public TagCmd(Vortex vortex) {
        super(vortex);
        this.name = "tag";
        this.guildOnly = true;
        this.arguments = "<tagName>";
        this.help = "displays a tag";
        this.options = Collections.singletonList(new OptionData(OptionType.STRING, "name", "the tags name", true));
    }


    protected void execute(HybridEvent e) {
        String tagName = e.isSlashCommandEvent() ? e.getSlashCommandEvent().optString("name") : e.getCommandEvent().getArgs();
        tagName = tagName.trim();

        if (tagName.isBlank()) {
            e.replyError("Please enter a valid tag name!");
            return;
        }

        String tagValue = vortex.getDatabase().tags.getTagValue(e.getGuild(), tagName);
        if (tagValue == null) {
            e.replyError("Tag " + tagName + " not found");
        } else {
            e.reply(tagValue);
        }
    }
}
