/*
 * Copyright 2018 John Grosh (john.a.grosh@gmail.com).
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
package com.jagrosh.vortex.utils;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.vortex.Emoji;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.User.UserFlag;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author John Grosh (john.a.grosh@gmail.com)
 */
@Slf4j
public class OtherUtil {

    public final static char[] DEHOIST_ORIGINAL = {'!', '"', '#', '$', '%', '&', '\'', '(', ')', '*', '+', ',', '-', '.', '/'};
    public final static char[] DEHOIST_REPLACEMENTS = {'\u01C3', '\u201C', '\u2D4C', '\uFF04', '\u2105',     // visually
            '\u214B', '\u2018', '\u2768', '\u2769', '\u2217', '\u2722', '\u201A', '\u2013', '\u2024', '\u2044'}; // similar
    public final static String DEHOIST_JOINED = "`" + FormatUtil.join("`, `", DEHOIST_ORIGINAL) + "`";

    public static boolean dehoist(Member m, char symbol) {
        if (!m.getGuild().getSelfMember().canInteract(m)) {
            return false;
        }

        if (m.getEffectiveName().charAt(0) > symbol) {
            return false;
        }

        String newname = m.getEffectiveName();
        for (int i = 0; i < DEHOIST_ORIGINAL.length; i++) {
            if (DEHOIST_ORIGINAL[i] == newname.charAt(0)) {
                newname = DEHOIST_REPLACEMENTS[i] + (newname.length() == 1 ? "" : newname.substring(1));
                break;
            }
        }

        m.getGuild().modifyNickname(m, newname).reason("Dehoisting").queue();
        return true;
    }

    // TODO: Potentially add CommandEvent#replyInDm(MessageCreateData,Consumer<Message>,Consumer<Throwable>) to chewtills
    public static void commandEventReplyDm(CommandEvent commandEvent, MessageCreateData message, Consumer<Message> success, Consumer<Throwable> failure) {
        if (commandEvent.isFromType(ChannelType.PRIVATE)) {
            commandEvent.getPrivateChannel().sendMessage(message).queue(success, failure);
        } else {
            commandEvent.getAuthor().openPrivateChannel().queue((pc) -> {
                pc.sendMessage(message).queue(success, failure);
            }, failure);
        }
    }

    public static void safeDM(User user, String message, boolean shouldDM, Runnable then) {
        if (user == null || !shouldDM) {
            then.run();
        } else {
            try {
                user.openPrivateChannel().queue(pc -> pc.sendMessage(message).queue(s -> then.run(), f -> then.run()), f -> then.run());
            } catch (Exception ignore) {
            }
        }
    }

    public static Member findMember(String username, String discriminator, Guild guild) {
        return guild.getMembers().stream().filter(m -> m.getUser().getName().equals(username) && m.getUser().getDiscriminator().equals(discriminator)).findAny().orElse(null);
    }

    public static Member getMemberCacheElseRetrieve(Guild g, long id) {
        Member m = g.getMemberById(id);
        return m != null ? m : g.retrieveMemberById(id).complete();
    }

    public static User getUserCacheElseRetrieve(JDA jda, long id) {
        User u = jda.getUserById(id);
        return u != null ? u : jda.retrieveUserById(id).complete();
    }

    public static int parseTime(String timestr) {
        timestr = timestr.replaceAll("(?i)(\\s|,|and)", "").replaceAll("(?is)(-?\\d+|[a-z]+)", "$1 ").trim();
        String[] vals = timestr.split("\\s+");
        int timeinseconds = 0;
        try {
            for (int j = 0; j < vals.length; j += 2) {
                int num = Integer.parseInt(vals[j]);

                // Parses units, if any
                if (vals.length > j + 1 && vals[j + 1].length() != 0) {
                    char timeUnit = vals[j + 1].toLowerCase().charAt(0);
                    num *= switch (timeUnit) {
                        case 'm' -> 60;
                        case 'h' -> 60 * 60;
                        case 'd' -> 60 * 60 * 24;
                        case 'w' -> 60 * 60 * 24 * 7;
                        case 'y' -> 60 * 60 * 24 * 365;
                        default  -> 1;
                    };
                }

                timeinseconds += num;
            }
        } catch (Exception ex) {
            return -1;
        }

        return timeinseconds;
    }

    public static boolean isFalse(Boolean b) {
        return b != null && !b;
    }

    public static boolean isTrue(Boolean b) {
        return b != null && b;
    }

    public static String[] readLines(String filename) {
        try {
            List<String> values = Files.readAllLines(Paths.get("lists" + File.separator + filename + ".txt")).stream().map(str -> str.replace("\uFEFF", "").trim()).filter(str -> !str.isEmpty() && !str.startsWith("//")).collect(Collectors.toList());
            String[] list = new String[values.size()];
            for (int i = 0; i < list.length; i++) {
                list[i] = values.get(i);
            }

            log.info("Successfully read " + list.length + " entries from '" + filename + "'");
            return list;
        } catch (Exception ex) {
            log.error("Failed to read '" + filename + "':" + ex);
            return new String[0];
        }
    }
}
