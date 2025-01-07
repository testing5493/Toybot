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
 * limitations under the License.
 */
package com.jagrosh.vortex.commands.general;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import com.jagrosh.jdautilities.commons.utils.FinderUtil;
import com.jagrosh.vortex.Constants;
import com.jagrosh.vortex.Emoji;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.commands.CommandTools;
import com.jagrosh.vortex.utils.DiscordPallete;
import com.jagrosh.vortex.utils.FormatUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.TimeFormat;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * @author John Grosh (jagrosh)
 */
// FIXME so this work with webhooks
public class UserInfoCmd extends SlashCommand {
    private final Vortex vortex;

    public UserInfoCmd(Vortex vortex) {
        this.name = "whois";
        this.aliases = new String[]{"whos", "user", "uinfo", "memberinfo", "userinfo", "whothis", "newphonewhothis"};
        this.help = "shows info on a member";
        this.arguments = "[user]";
        this.guildOnly = true;
        this.vortex = vortex;
        this.options = Collections.singletonList(new OptionData(OptionType.USER, "user", "The user", true));
    }

    @Override
    protected void execute(SlashCommandEvent event) {
        if (!CommandTools.hasGeneralCommandPerms(vortex, event, Permission.MESSAGE_MANAGE)) {
            event.reply(CommandTools.COMMAND_NO_PERMS).setEphemeral(true).queue();
            return;
        }

        User u = event.getOption("user").getAsUser();
        Member m = null;
        if (event.getGuild() != null) {
            try {
                m = event.getGuild().retrieveMember(u).complete();
            } catch (ErrorResponseException ignore) {
            }
        }

        event.replyEmbeds(generateInfoEmbed(u, m)).queue();
    }

    @Override
    protected void execute(CommandEvent event) {
        if (!CommandTools.hasGeneralCommandPerms(vortex, event, Permission.MESSAGE_MANAGE)) {
            return;
        }

        JDA jda = event.getJDA();
        String args = event.getArgs();
        final String QUERY = args.trim().replaceAll("@", "");

        // Potential fields that may be filled in by search
        Member m = null;
        User u = null;
        long id = -1;

        // Tries to find member/user/id etc.
        if (QUERY.isEmpty()) {
            m = event.getMember();
        } else if (QUERY.matches("<@?!?\\d{17,20}>") || QUERY.matches("\\d{17,20}")) {
            id = Long.parseLong(QUERY.replaceAll("\\D", ""));

            if (id == Constants.DELETED_USER_ID) {
                //TODO: Implement maybe
            }
        } else if (QUERY.matches(FinderUtil.FULL_USER_REF.pattern())) {
            Matcher tagMatcher = FinderUtil.FULL_USER_REF.matcher(event.getArgs());
            String username = tagMatcher.group(1);
            String discrim = tagMatcher.group(2);

            if (username.equals("Deleted User") && discrim.matches("0+")) {
                //TODO: Implement maybe
            }

            u = jda.getUserByTag(tagMatcher.group(1), tagMatcher.group(2));
        } else {
            List<User> users = jda.getUsers();
            List<User> potentialUsers = matchName(users, QUERY, User::getName, null);


            boolean mulitpleFound = false;
            if (potentialUsers.isEmpty()) {
                potentialUsers = matchName(users, QUERY, User::getGlobalName, usr -> usr.getGlobalName() != null && !usr.getGlobalName().equals(usr.getName()));
                if (potentialUsers.isEmpty()) {
                    List<Member> potentialMembers = matchName(event.getGuild().getMembers(), QUERY, Member::getNickname, mbr -> mbr.getNickname() != null);
                    if (potentialMembers.isEmpty()) {
                        // TODO: Potentially search from modlog records for matching user id and retrieve that
                        event.replyError("A user with that name could not be found. Please double check your spelling or enter an ID or mention");
                        return;
                    } else if (potentialMembers.size() == 1) {
                        m = potentialMembers.get(0);
                    } else {
                        mulitpleFound = true;
                    }
                }
            }

            if (potentialUsers.size() == 1) {
                u = potentialUsers.get(0);
            } else if (potentialUsers.size() > 1 || mulitpleFound) {
                event.replyError("Multiple users with that name were found. Please be more percise or enter an ID or mention");
                return;
            }
        }

        if (id != -1) {
            try {
                 u = jda.retrieveUserById(id).complete();
            } catch (Exception ignore) {}

            if (u == null) {
                event.replyError("A user with that ID could not be found");
            }
        }

        if (u != null && m == null) {
            m = event.getGuild().getMember(u);
        } else if (m != null && u == null) {
            u = m.getUser();
        }

        event.reply(generateInfoEmbed(u, m));
    }

    private static String formatActivity(Activity activity) {
        if (activity.getType() == Activity.ActivityType.STREAMING) {
            return "Streaming [*" + activity.getName() + "*](" + activity.getUrl() + ")";
        } else {
            String verb = switch (activity.getType()) {
                case LISTENING -> "Listening to";
                case WATCHING -> "Watching";
                case COMPETING -> "Competing in";
                default -> "Playing";
            };

            return activity.getType() == Activity.ActivityType.CUSTOM_STATUS ? activity.getName() : verb + " *" + activity.getName() + "*";
        }
    }

    private static MessageEmbed generateInfoEmbed(User u, Member m) {
        if (u == null && m != null) {
            u = m.getUser();
        }

        User.Profile p = u.retrieveProfile().complete();
        String username = u.getName();
        int discrim = 0;
        try {
            discrim = Integer.parseInt(u.getDiscriminator());
        } catch (NumberFormatException ignore) {}
        username = discrim == 0 ? "@" + username : u.getAsTag();

        StringBuilder badges = new StringBuilder();
        if (u.isBot() || u.isSystem()) {
            badges.append(' ');
            if (u.isSystem()) {
                badges.append(Emoji.VERIFIED_SYSTEM);
            } else if (u.getIdLong() == Constants.CLYDE_AI_ID) {
                 badges.append(Emoji.VERIFIED_AI);
            } else if (u.getFlags().contains(User.UserFlag.VERIFIED_BOT)) {
                badges.append(Emoji.VERIFIED_BOT);
            } else {
                badges.append(Emoji.BOT);
            }
        }

        badges.append((m != null && m.isOwner()) ? Emoji.SERVER_OWNER : "")
              .append(u.getFlags().contains(User.UserFlag.STAFF) || u.getIdLong() == Constants.DISCORD_COMMUNITY_UPDATES_ID ? Emoji.DISCORD_STAFF : "");

        for (User.UserFlag flag : u.getFlags()) {
            System.out.println(flag.getName());
            badges.append(switch (flag) {
                case PARTNER -> Emoji.PARTNERED_USER;
                case CERTIFIED_MODERATOR -> Emoji.MODERATOR_ALUMNI;
                case VERIFIED_DEVELOPER -> Emoji.VERIFIED_EARLY_DEV;
                case ACTIVE_DEVELOPER -> Emoji.ACTIVE_DEVELOPER;
                case BUG_HUNTER_LEVEL_1 -> Emoji.BUG_HUNTER_LEVEL_1;
                case BUG_HUNTER_LEVEL_2 -> Emoji.BUG_HUNTER_LEVEL_2;
                case EARLY_SUPPORTER -> Emoji.EARLY_NITRO_SUB;
                case HYPESQUAD -> Emoji.HYPESQUAD_EVENTS;
                case HYPESQUAD_BALANCE -> Emoji.HYPESQUAD_BALANCE;
                case HYPESQUAD_BRAVERY -> Emoji.HYPESQUAD_BRAVERY;
                case HYPESQUAD_BRILLIANCE -> Emoji.HYPESQUAD_BRILIANCE;
                default -> "";
            });
        }

        badges.append(m != null && OffsetDateTime.now().minusWeeks(1).isBefore(m.getTimeJoined()) ? Emoji.NEW_MEMBER : "");


        List<String> formattedActivities = new ArrayList<>();
        if (m != null) {
            for (Activity activity : m.getActivities()) {
                if (activity != null) {
                    formattedActivities.add(formatActivity(activity));
                }
            }
        }

        EmbedBuilder builder = new EmbedBuilder();
        builder.getDescriptionBuilder().append(FormatUtil.formatList(formattedActivities, ", "));

        builder.setTitle(String.format("Showing Info For %s %s", username, badges)).setColor((m != null && m.getColor() != null) ? m.getColor() : DiscordPallete.DEFAULT_ROLE_WHITE).setThumbnail(m == null ? u.getEffectiveAvatarUrl() : m.getEffectiveAvatarUrl()).addField("ID", u.getId(), true).addField("Created At", TimeFormat.DATE_TIME_SHORT.format(u.getTimeCreated()), true);

        if (m != null) {
            builder.addField("Joined At", TimeFormat.DATE_TIME_SHORT.format(m.getTimeJoined()), true);
        }

        builder.addField("Images", getIconURLList(m, u, p), true);

        if (m != null && m.getJDA().getCacheFlags().contains(CacheFlag.ONLINE_STATUS)) {
            StringBuilder statusBuilder = new StringBuilder().append(Constants.ZWSP); // ZWSP for mobile rendering
            switch (m.getOnlineStatus(ClientType.DESKTOP)) {
                case ONLINE -> statusBuilder.append(Emoji.DESKTOP_ONLINE);
                case IDLE -> statusBuilder.append(Emoji.DESKTOP_IDLE);
                case DO_NOT_DISTURB -> statusBuilder.append(Emoji.DESKTOP_DND);
                case OFFLINE, INVISIBLE -> statusBuilder.append(Emoji.DESKTOP_OFFLINE);
            }

            switch (m.getOnlineStatus(ClientType.MOBILE)) {
                case ONLINE -> statusBuilder.append(Emoji.MOBILE_ONLINE);
                case IDLE -> statusBuilder.append(Emoji.MOBILE_IDLE);
                case DO_NOT_DISTURB -> statusBuilder.append(Emoji.MOBILE_DND);
                case OFFLINE, INVISIBLE -> statusBuilder.append(Emoji.MOBILE_OFFLINE);
            }

            switch (m.getOnlineStatus(ClientType.WEB)) {
                case ONLINE -> statusBuilder.append(Emoji.BROWSER_ONLINE);
                case IDLE -> statusBuilder.append(Emoji.BROWSER_IDLE);
                case DO_NOT_DISTURB -> statusBuilder.append(Emoji.BROWSER_DND);
                case OFFLINE, INVISIBLE -> statusBuilder.append(Emoji.BROWSER_OFFLINE);
            }

            builder.addField("Status", statusBuilder.toString(), true);
        }

        if (p.getAccentColor() != null && p.getBanner() == null) {
            builder.addField("Accent Colour", FormatUtil.formatColor(p.getAccentColorRaw()), true);
        }

        if (m != null && m.isBoosting()) {
            builder.addField("Boosting Since", TimeFormat.DATE_TIME_SHORT.format(m.getTimeBoosted()), true);
            badges.append(Emoji.SERVER_BOOSTER);
        }

        if (m != null) {
            String rolesFormatted = FormatUtil.listOfRolesMention(m.getRoles()).trim();
            builder.addField("Roles", rolesFormatted.isEmpty() ? "None" : rolesFormatted, false);
        }

        return builder.build();
    }

    private static String getIconURLList(Member m, User u, User.Profile p) {
        FormatUtil.IconURLFieldBuilder ibuilder = new FormatUtil.IconURLFieldBuilder();
        if (m != null && m.getAvatarUrl() != null) {
            ibuilder.add("Server PFP", m.getAvatarUrl());
        }

        ibuilder.add("PFP", u.getEffectiveAvatarUrl() + "?size=4096");
        if (p.getBannerUrl() != null) {
            ibuilder.add("Banner", p.getBannerUrl() + "?size=4096");
        }

        return ibuilder.toString();
    }

    // TODO: Double check this works properly because I written this while very tired
    private static <T> List<T> matchName(List<T> objs, String name, Function<T, String> nameMap, Predicate<T> initialFilter) {
        String desymboled = desymbol(name);
        boolean symbolHeavy = !desymboled.isEmpty() && name.length() /desymboled.length() >= 2;
        String effectiveNameInsensitive = (symbolHeavy ? desymboled : name).toLowerCase();
        Predicate<String> containsName = s -> s.toLowerCase().contains(effectiveNameInsensitive);

        Stream<T> stream = objs.parallelStream();
        if (initialFilter != null) {
            stream = stream.filter(initialFilter);
        }

        List<InterimName<T>> filtered = stream.map(t -> new InterimName<T>(t, nameMap.apply(t), symbolHeavy ? desymbol(nameMap.apply(t)) : null)).filter(iName -> containsName.test(!symbolHeavy ? iName.name() : iName.desymboled())).toList();

        if (filtered.size() <= 1) {
            return toTList(filtered);
        }

        if (symbolHeavy && !desymboled.equals(name)) {
            filtered = getMatchingNames(filtered, iName -> desymboled.equalsIgnoreCase(iName.desymboled()));
            if (objs.size() <= 1) {
                return toTList(filtered);
            }

            filtered = getMatchingNames(filtered, iName -> desymboled.equals(iName.desymboled()));
            if (objs.size() <= 1) {
                return toTList(filtered);
            }
        }

        filtered = getMatchingNames(filtered, iName -> name.equalsIgnoreCase(iName.name()));
        if (objs.size() <= 1) {
            return toTList(filtered);
        }

        filtered = getMatchingNames(filtered, iName -> name.equals(iName.name()));
        if (objs.size() <= 1) {
            return toTList(filtered);
        }

        return toTList(filtered);
    }

    private static <T> List<InterimName<T>> getMatchingNames(List<InterimName<T>> objs, Predicate<InterimName<T>> predicate) {
        LinkedList<InterimName<T>> filteredList = new LinkedList<>();
        for (InterimName<T> iName : objs) {
            if (predicate.test(iName)) {
                filteredList.add(iName);
            }
        }

        return filteredList;
    }

    private static String desymbol(String str) {
        return str.replaceAll("[^a-zA-Z]", "");
    }

    private static <T> List<T> toTList(List<InterimName<T>> list) {
        return list.parallelStream().map(InterimName::object).toList();
    }

    private record InterimName<T>(T object, String name, String desymboled) {}
}
