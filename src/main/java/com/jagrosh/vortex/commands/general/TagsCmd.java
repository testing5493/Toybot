package com.jagrosh.vortex.commands.general;

import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.commands.HybridEvent;
import com.jagrosh.vortex.utils.FormatUtil;

import java.util.List;

public class TagsCmd extends GeneralHybridCmd {
    public TagsCmd(Vortex vortex) {
        super(vortex);
        this.name = "tags";
        this.arguments = "";
        this.help = "lists all tags";
        this.guildOnly = true;
    }

    @Override
    protected void execute(HybridEvent e) {
        List<String> tags = vortex.getDatabase().tags.getTagNames(e.getGuild());
        tags.sort(String::compareTo);

        if (tags.isEmpty()) {
            e.reply("There are no tags on this server");
        } else {
            e.reply(FormatUtil.capitalize(FormatUtil.formatList(tags, ", ")));
        }
    }
}
