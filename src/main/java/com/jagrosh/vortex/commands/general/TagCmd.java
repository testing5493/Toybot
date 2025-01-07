package com.jagrosh.vortex.commands.general;

import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.commands.HybridEvent;
import jakarta.persistence.PersistenceException;
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
        try {
            String tagName = e.isSlashCommandEvent() ? e.getSlashCommandEvent().optString("name") : e.getCommandEvent().getArgs();
            tagName = tagName.trim().toLowerCase();

            String tagValue = vortex.getHibernate().tags.getTag(e.getGuild().getIdLong(), tagName);

            if (tagValue == null) {
                e.replyError("Tag " + tagName + " not found");
            } else {
                e.reply(tagValue);
            }
        } catch (PersistenceException ex) {
            e.replyError("An error occurred retrieving the tag. Please try again");
        } catch (IllegalArgumentException ex) {
            e.replyError("Please enter a valid tag name");
        }
    }
}
