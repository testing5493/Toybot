package com.jagrosh.vortex.commands.general;

import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.commands.HybridEvent;
import com.jagrosh.vortex.utils.FormatUtil;
import jakarta.persistence.PersistenceException;

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
        try {
            List<String> tags = vortex.getHibernate().tags.getTags(e.getGuild().getIdLong());
            tags.sort(String::compareTo);

            if (tags.isEmpty()) {
                e.reply("There are no tags on this server");
            } else {
                e.reply(FormatUtil.capitalize(FormatUtil.formatList(tags, ", ")));
            }
        } catch (PersistenceException ex) {
            e.replyError("An error occurred retrieving the tags. Please try again.");
        }
    }
}
