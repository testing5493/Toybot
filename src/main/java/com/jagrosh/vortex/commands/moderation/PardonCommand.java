package com.jagrosh.vortex.commands.moderation;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import com.jagrosh.vortex.Action;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.commands.CommandExceptionListener;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class PardonCommand extends ModCommand {
    protected Action action;
    public PardonCommand(Vortex vortex, Action action, Permission... altPerms) {
        super(vortex, altPerms);
        this.action = action;
        this.arguments = "<@user>";
        this.guildOnly = true;
        this.options = List.of(new OptionData(OptionType.USER, "user", "The user to " + action.getVerb(), true));
    }

    @Override
    protected final void execute1(SlashCommandEvent event) {
        User targetUser = event.getOption("user", OptionMapping::getAsUser);

        try {
            String success = execute(event.getMember(), targetUser.getIdLong());
            event.reply(success).queue();
        } catch (CommandExceptionListener.CommandErrorException e) {
            event.reply(e.getMessage()).setEphemeral(true).queue();
        }
    }

    @Override
    protected final void execute(CommandEvent event) {
        try {
            String success = execute(event.getMember(), parseId(event.getArgs()));
            event.reply(success);
        } catch (IllegalArgumentException e) {
            throw new CommandExceptionListener.CommandErrorException("The format for this command is " + arguments);
        }
    }

    protected abstract String execute(Member mod, long targetId);

    private static long parseId(String args) throws IllegalArgumentException {
        args = args.trim();
        final Pattern USER_MENTION_PATTERN = Pattern.compile("^<@!?(\\d{17,20})>");
        final Pattern USER_ID_PATTERN = Pattern.compile("^(\\d{17,20})");

        boolean requiresSpaceAfterFirstArg = false; // This should be true if the first thing is an ID and not a mention, since you need a space after an ID but not a mention. However this field is ignored if the id is the only thing in the arguments
        Matcher m = USER_MENTION_PATTERN.matcher(args);
        if (!m.matches()) {
            m = USER_ID_PATTERN.matcher(args);
            if (!m.matches()) {
                throw new IllegalArgumentException();
            }

            requiresSpaceAfterFirstArg = true;
        }

        long id = Long.parseLong(m.group().replaceAll("\\D", ""));
        if (requiresSpaceAfterFirstArg && args.length() > m.group().length()) {
            throw new IllegalArgumentException();
        }

        return id;
    }
}
