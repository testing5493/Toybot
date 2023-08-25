package com.jagrosh.vortex.commands.moderation;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import com.jagrosh.vortex.Action;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.commands.CommandExceptionListener;
import com.jagrosh.vortex.commands.HybridEvent;
import com.jagrosh.vortex.utils.OtherUtil;
import lombok.Value;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.jagrosh.vortex.utils.ArgsUtil.TIME_REGEX;

public abstract class PunishmentCmd extends ModCommand {
    protected boolean timed;
    protected Action action;
    public PunishmentCmd(Vortex vortex, Action action, boolean timed, Permission... altPerms) {
        super(vortex, altPerms);
        this.action = action;
        this.timed = timed;
        this.arguments = String.format("<@user> %s [reason]", timed ? "[timed]" : "");
        this.guildOnly = true;

        String verb = action.getVerb();
        List<OptionData> optionList = new LinkedList<>();
        optionList.add(new OptionData(OptionType.USER, "user", "The user to " + verb, true));

        if (timed) {
            optionList.add(new OptionData(OptionType.STRING, "time", "The amount of time to " + verb + " for", false));
        }

        optionList.add(new OptionData(OptionType.STRING, "reason", "The reason for " + action.getPresentVerb(), false));

        this.options = optionList;
    }

    @Override
    public final void execute1(SlashCommandEvent e) {
        User targetUser = e.getOption("user", OptionMapping::getAsUser);
        int time = parseMinutes(e.getOption("time", OptionMapping::getAsString));
        String reason = e.getOption("reason", OptionMapping::getAsString);

        HybridEvent hybridEvent = HybridEvent.of(e);

        try {
            execute(hybridEvent, targetUser.getIdLong(), time, reason);
        } catch (CommandExceptionListener.CommandErrorException ex) {
            hybridEvent.replyError(ex.getMessage());
        }
    }

    @Override
    public final void execute(CommandEvent e) {
        HybridEvent hybridEvent = HybridEvent.of(e);

        try {
            PunishmentCommandArgs args = parsePunishmentCommandArgs(e.getArgs());
            execute(hybridEvent, args.getUserId(), args.getMinutes(), args.getReason());
        } catch (IllegalArgumentException ex) {
            hybridEvent.replyError("The format for this command is " + arguments);
        } catch (CommandExceptionListener.CommandErrorException ex) {
            hybridEvent.replyError(ex.getMessage());
        }
    }

    /**
     * The actual logic of the punishment command to execute. In general, the control flow goes:
     * 1. Check if all parties involved have required perms
     * 2. Attempt to punish the user on discord
     * 3. Attempt to log the punishment in the database, modlogs channel, as well as the executed channel/target's dms if the command isn't silent
     * @param event The {@link HybridEvent} event
     * @param userId The target user Id
     * @param min The minutes to punish for, or -1 if no time was specified
     * @param reason The reason for punishing the user, or blank for no reason
     */
    // TODO: fully implement step 3
    protected abstract void execute(HybridEvent event, long userId, int min, String reason);

    /**
     * Parses a user supplied argument for punishment commands, such as ?ban or ?gravel.
     * The format for this is [mention] (time) (reason). Mention is a required argument while time and reason are optional.
     *
     * @param args The user supplied arguments. This does not need to be trimmed or escaped.
     * @return An instance of {@link PunishmentCommandArgs}
     * @throws IllegalArgumentException If the specified arguments are illegal
     */
    // todo: make it so args is not trimmed with the standard String api to make sure no one does any funny things with control characters ie, ?ban[\u0000]320453049530459 as a joke and it actually banning the person
    // I'm looking at you bitslayn
    private PunishmentCommandArgs parsePunishmentCommandArgs(String args) throws IllegalArgumentException {
        // TODO: Properly parse arguments
        args = args.trim();
        final Pattern USER_MENTION_PATTERN = Pattern.compile("^<@!?(\\d{17,20})>");
        final Pattern USER_ID_PATTERN = Pattern.compile("^(\\d{17,20})");

        boolean requiresSpaceAfterFirstArg = false; // This should be true if the first thing is an ID and not a mention, since you need a space after an ID but not a mention. However this field is ignored if the id is the only thing in the arguments
        Matcher m = USER_MENTION_PATTERN.matcher(args);
        if (!m.find()) {
            m = USER_ID_PATTERN.matcher(args);
            if (!m.find()) {
                throw new IllegalArgumentException();
            }

            requiresSpaceAfterFirstArg = true;
        }

        long id = Long.parseLong(m.group().replaceAll("\\D", ""));

        if (args.length() == m.end() - m.start()) {
            return new PunishmentCommandArgs(id, -1, "");
        }

        args = args.substring(m.end());
        if (args.isBlank()) {
            return new PunishmentCommandArgs(id, -1, "");
        }

        if (requiresSpaceAfterFirstArg) {
            if (args.charAt(0) != ' ') {
                throw new IllegalArgumentException();
            }
        }
        args = args.trim();

        // TODO: Figure out how the heck this thing works (?????)
        int seconds, min = -1;
        if (timed) {
            String timeString = args.replaceAll(TIME_REGEX, "$1");
            if (!timeString.isEmpty()) {
                String argsSave = args; // In case something went wrong parsing
                args = args.substring(timeString.length()).trim();
                seconds = OtherUtil.parseTime(timeString);

                if (seconds <= -1) {
                    args = argsSave; // Something went wrong, treat "time" as the reason
                } else {
                    // Round to the nearest minute
                    min = seconds / 60;
                    if (seconds - 60 * min >= 30) {
                        ++min;
                    }
                }
            }
        }

        return new PunishmentCommandArgs(id, min, args);
    }

    @Value
    private static class PunishmentCommandArgs {
        long userId;
        int minutes;
        String reason;
    }

    private static int parseMinutes(String rawTime) {
        int min = -1;
        if (rawTime == null) {
            return -1;
        }

        String timeString = rawTime.replaceAll(TIME_REGEX, "$1");
        if (!timeString.isEmpty()) {
            int seconds = OtherUtil.parseTime(timeString);

            if (seconds >= 0) {
                min = seconds / 60;
                if (seconds - 60 * min >= 30) {
                    ++min;
                }
            }
        }

        return min;
    }
}
