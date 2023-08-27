package com.jagrosh.vortex.commands.moderation;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import com.jagrosh.vortex.Action;
import com.jagrosh.vortex.Emoji;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.commands.CommandTools;
import com.jagrosh.vortex.database.Database;
import com.jagrosh.vortex.database.Database.Modlog;
import com.jagrosh.vortex.hibernate.entities.BanLog;
import com.jagrosh.vortex.hibernate.entities.ModLog;
import com.jagrosh.vortex.utils.FormatUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import javax.xml.crypto.Data;
import java.util.Collections;
import java.util.List;

// TODO: Potentially merge this with the case command?
// TODO: make this paged because right now it only supports 250 modlogs
public class ModlogsCmd extends ModCommand {
    public ModlogsCmd(Vortex vortex) {
        super(vortex, Permission.MANAGE_ROLES);
        this.name = "modlogs";
        this.arguments = "[@user]";
        this.help = "shows modlogs for a user or retrieves info about a modlog";
        this.guildOnly = true;
        this.options = Collections.singletonList(new OptionData(OptionType.USER, "user", "The user to look up modlogs for", false));
    }

    @Override
    protected void execute1(SlashCommandEvent event) {
        User u = event.getOption("user", OptionMapping::getAsUser);
        if (u == null) {
            u = event.getUser();
        }

        event.reply(generateMessage(event.getGuild(), u.getIdLong(), u)).queue();
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getArgs().equalsIgnoreCase("help")) {
            event.replySuccess("This command is used to see a user's modlogs for the current server. Please mention a user or use a user ID to check their modlogs.");
            return;
        }

        long userId = CommandTools.getPossibleUserId(event.getArgs().trim());
        if (userId == -1) {
            if (event.getArgs().trim().isEmpty()) {
                userId = event.getAuthor().getIdLong();
            } else {
                event.reply("Please mention someone or enter a valid user ID");
                return;
            }
        }

        event.reply(generateMessage(event.getGuild(), userId, null));
    }

    private MessageCreateData generateMessage(Guild g, long userId, User u) {
        List<ModLog> modlogs = vortex.getHibernate().modlogs.getCases(g.getIdLong(), userId);
        int size = modlogs.size();
        if (size == 0) {
            return MessageCreateData.fromContent("Could not find any modlogs for that user");
        }

        EmbedBuilder[] embeds = new EmbedBuilder[size / 25 + 1];
        for (int i = 0; i < embeds.length; i++) {
            embeds[i] = new EmbedBuilder();
        }

        if (u == null) {
            u = g.getJDA().getUserById(userId);
            if (u == null) {
                u = g.getJDA().retrieveUserById(userId).complete();
            }
        }

        if (u != null) {
            embeds[0].setAuthor(String.format("%d modlog%s found for %s", size, size == 1 ? "" : "s", FormatUtil.formatFullUser(u)), null, u.getEffectiveAvatarUrl());
        } else {
            embeds[0].setAuthor(String.format("%d modlog%s found for %d", size, size == 1 ? "" : "s", userId));
        }

        for (int i = 0; i < modlogs.size(); i++) {
            ModLog modlog = modlogs.get(i);
            embeds[i / 25].addField(modlog.actionType().getEmoji().neutralEmoji() + " Case: " + modlog.getCaseId(), FormatUtil.formatModlogCase(vortex, g, modlog), false);
        }

        MessageCreateBuilder messageBuilder = new MessageCreateBuilder();
        for (EmbedBuilder embed : embeds) {
            messageBuilder.addEmbeds(embed.build());
        }

        return messageBuilder.build();
    }
}