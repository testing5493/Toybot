/*
 * Copyright 2016 John Grosh (jagrosh).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. Furthermore, I'm putting this sentence in all files because I messed up git and its not showing files as edited -\\_( :) )_/-
 */
package com.jagrosh.vortex.commands.moderation;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.commands.CommandExceptionListener.CommandErrorException;
import com.jagrosh.vortex.utils.FormatUtil;
import com.jagrosh.vortex.utils.LogUtil;
import com.jagrosh.vortex.utils.OtherUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * @author Michaili K (mysteriouscursor+git@protonmail.com)
 */
// TODO: Make it so you can optionally manage different channels than the one you're in
// TODO: Add a submcommand to turn it on or off without specifying a time for slash commands
public class SlowmodeCmd extends ModCommand {
    private final static int MAX_SLOWMODE = 21600;

    public SlowmodeCmd(Vortex vortex) {
        super(vortex, Permission.MANAGE_CHANNEL);
        this.name = "slowmode";
        this.arguments = "[time or OFF] | [time to disable slowmode]";
        this.help = "enables or disables slowmode";
        this.botPermissions = new Permission[]{Permission.MANAGE_CHANNEL};

        this.options = List.of(
                new OptionData(OptionType.STRING, "time", "How much time users must wait to send a message (ie 5s)", false),
                new OptionData(OptionType.STRING, "duration", "Time until slowmode is disabled (ie 30 min). Leave blank for indefinte", false)
        );
    }

    @Override
    protected void execute(SlashCommandEvent event) {
        String timeStr = event.getOption("time", OptionMapping::getAsString);
        String durStr = event.getOption("duration", OptionMapping::getAsString);

        try {
            String message = execute(event.getTextChannel(), event.getMember(), timeStr, durStr);
            event.reply(message).queue();
        } catch (CommandErrorException e) {
            event.reply(e.getMessage()).setEphemeral(true).queue();
        }
    }

    @Override
    protected void execute(CommandEvent event) {
        TextChannel tc = event.getTextChannel();
        Member mod = event.getMember();

        String[] split = event.getArgs().split("\\|", 2);

        String message = switch (split.length) {
            case 0 -> execute(tc, mod, null, null);
            case 1 -> execute(tc, mod, split[0], null);
            default -> execute(tc, mod, split[0], split[1]);
        };

        event.reply(message);
    }


    private String execute(TextChannel tc, Member mod, String timeString, String durString) {
        timeString = timeString == null ? "" : timeString.toLowerCase().trim();
        durString  = durString  == null ? "" : durString.toLowerCase().trim();

        if (timeString.isBlank() && durString.isBlank()) {
            int slowmodeDuration = vortex.getDatabase().tempslowmodes.timeUntilDisableSlowmode(tc);
            int slowmodeTime = tc.getSlowmode();

            if (slowmodeTime <= 0) {
                return "Slowmode is disabled.";
            }

            return "Slowmode is enabled with 1 message every " + FormatUtil.secondsToTimeCompact(slowmodeTime) + (slowmodeDuration <= 0 ? "." : " for " + FormatUtil.secondsToTimeCompact(slowmodeDuration) + ".");
        }

        Function<String, Boolean> isEnableDisable = str -> switch(str) {
            case "on", "enabled", "enable", "true" -> true;
            case "off", "disabled", "disable", "false" -> false;
            default -> null;
        };

        Boolean timeIsEnabled = isEnableDisable.apply(timeString);
        Boolean durationIsEnabled = isEnableDisable.apply(durString);

        if (OtherUtil.isFalse(timeIsEnabled) || OtherUtil.isFalse(durationIsEnabled)) {
            vortex.getDatabase().tempslowmodes.clearSlowmode(tc);
            tc.getManager().setSlowmode(0).reason(LogUtil.auditReasonFormat(mod, "Disabled slowmode")).queue();
            return "Disabled slowmode!";
        }

        int slowmodeTime = OtherUtil.parseTime(timeString);
        int slowmodeDuration;
        if (slowmodeTime == -1) {
            throw new CommandErrorException("Invalid slowmode time!");
        }

        if (slowmodeTime > MAX_SLOWMODE) {
           throw new CommandErrorException("You can only enable slowmode for up to 6 hours!");
        }

        if (slowmodeTime < -1) {
            throw new CommandErrorException("Slowmode cannot use negative time!");
        }

        if (!durString.isBlank()) {
            slowmodeDuration = OtherUtil.parseTime(durString);

            if (slowmodeDuration == -1) {
                throw new CommandErrorException("Invalid slowmode duration time!");
            }

            if (slowmodeDuration < -1) {
                throw new CommandErrorException("Slowmode duration cannot use negative time!");
            }
        } else {
            slowmodeDuration = -1;
        }

        try {
            tc.getManager().setSlowmode(slowmodeTime).reason(LogUtil.auditReasonFormat(mod, slowmodeDuration / 60, "Enabled slowmode")).complete();
        } catch (Exception e) {
            throw new CommandErrorException("Oops! Something went wrong setting the slowmode");
        }

        if (slowmodeDuration > 0) {
            vortex.getThreadpool().schedule(() -> vortex.getDatabase().tempslowmodes.setSlowmode(tc, Instant.now().plus(slowmodeDuration, ChronoUnit.SECONDS)), 10, TimeUnit.SECONDS);
        }

        return String.format("Enabled slowmode with 1 message every %s%s",
                       FormatUtil.secondsToTimeCompact(slowmodeTime),
                       slowmodeDuration > 0 ? " for " + FormatUtil.secondsToTimeCompact(slowmodeDuration) : "."
        );
    }
}
