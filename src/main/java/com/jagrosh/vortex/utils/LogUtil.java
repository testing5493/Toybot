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

import com.jagrosh.vortex.logging.MessageCache.CachedMessage;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.sharding.ShardManager;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class LogUtil {
    // Constants
    private final static String NO_REASON = "[no reason specified]";

    public static String logMessagesForwards(String title, List<Message> messages) {
        TextChannel deltc = messages.get(0).getChannel().asTextChannel();
        Guild delg = messages.get(0).getGuild();
        StringBuilder sb = new StringBuilder("-- " + title + " -- #" + deltc.getName() + " (" + deltc.getId() + ") -- " + delg.getName() + " (" + delg.getId() + ") --");
        Message m;
        for (Message message : messages) {
            appendMessage(sb, message);
        }

        return sb.toString().trim();
    }

    public static String logCachedMessagesForwards(String title, List<CachedMessage> messages, JDA jda) {
        TextChannel deltc = messages.get(0).getTextChannel(jda);
        Guild delg = deltc.getGuild();
        StringBuilder sb = new StringBuilder("-- " + title + " -- #" + deltc.getName() + " (" + deltc.getId() + ") -- " + delg.getName() + " (" + delg.getId() + ") --");
        for (CachedMessage message : messages) {
            appendMessage(sb, message, message.getAuthor(jda));
        }

        return sb.toString().trim();
    }

    public static String logCachedMessagesForwards(String title, List<CachedMessage> messages, ShardManager shardManager) {
        TextChannel deltc = messages.get(0).getTextChannel(shardManager);
        Guild delg = deltc.getGuild();
        StringBuilder sb = new StringBuilder("-- " + title + " -- #" + deltc.getName() + " (" + deltc.getId() + ") -- " + delg.getName() + " (" + delg.getId() + ") --");
        for (CachedMessage message : messages) {
            appendMessage(sb, message, message.getAuthor(shardManager));
        }

        return sb.toString().trim();
    }

    public static String logMessagesBackwards(String title, List<Message> messages) {
        TextChannel deltc = messages.get(0).getChannel().asTextChannel();
        Guild delg = messages.get(0).getGuild();
        StringBuilder sb = new StringBuilder("-- " + title + " -- #" + deltc.getName() + " (" + deltc.getId() + ") -- " + delg.getName() + " (" + delg.getId() + ") --");
        Message m;
        for (int i = messages.size() - 1; i >= 0; i--) {
            appendMessage(sb, messages.get(i));
        }

        return sb.toString().trim();
    }

    private static void appendMessage(StringBuilder sb, Message m) {
        sb.append("\r\n\r\n[").append(m.getTimeCreated().format(DateTimeFormatter.RFC_1123_DATE_TIME)).append("] ").append(m.getAuthor().getName()).append("#").append(m.getAuthor().getDiscriminator()).append(" (").append(m.getAuthor().getId()).append(") : ").append(m.getContentRaw());
        m.getAttachments().forEach(att -> sb.append("\n").append(att.getUrl()));
    }

    private static void appendMessage(StringBuilder sb, CachedMessage m, User author) {
        sb.append("\r\n\r\n[").append(m.getTimeCreated().format(DateTimeFormatter.RFC_1123_DATE_TIME)).append("] ");
        if (author == null) {
            sb.append(m.getUsername()).append("#").append(m.getDiscriminator()).append(" (").append(m.getAuthorId());
        } else {
            sb.append(author.getName()).append("#").append(author.getDiscriminator()).append(" (").append(author.getId());
        }

        sb.append(") : ").append(m.getContentRaw());
        m.getAttachments().forEach(att -> sb.append("\n").append(att.getUrl()));
    }

    // Audit logging formats
    private final static String A_MOD = "%s#%s";
    private final static String A_TIME = " (%dm)";
    private final static String A_REASON = ": %s";

    private final static String AUDIT_BASIC_FORMAT = A_MOD + A_REASON;
    private final static String AUDIT_TIMED_FORMAT = A_MOD + A_TIME + A_REASON;
    private final static Pattern AUDIT_BASIC_PATTERN = Pattern.compile("^(\\S.{0,32}\\S)#(\\d{4}): (.*)$", Pattern.DOTALL);
    private final static Pattern AUDIT_TIMED_PATTERN = Pattern.compile("^(\\S.{0,32}\\S)#(\\d{4}) \\((\\d{1,9})m\\): (.*)$", Pattern.DOTALL);

    // Auditlog methods
    public static String auditReasonFormat(Member moderator, String reason) {
        return limit512(String.format(AUDIT_BASIC_FORMAT, moderator.getUser().getName(), moderator.getUser().getDiscriminator(), reasonF(reason)));
    }

    public static String auditReasonFormat(Member moderator, int minutes, String reason) {
        if (minutes <= 0) {
            return auditReasonFormat(moderator, reason);
        }

        return limit512(String.format(AUDIT_TIMED_FORMAT, moderator.getUser().getName(), moderator.getUser().getDiscriminator(), minutes, reasonF(reason)));
    }

    public static ParsedAuditReason parse(Guild guild, String reason) {
        if (reason == null || reason.isEmpty()) {
            return null;
        }

        try {
            // first try the timed pattern
            Matcher m = AUDIT_TIMED_PATTERN.matcher(reason);
            if (m.find()) {
                Member mem = OtherUtil.findMember(m.group(1), m.group(2), guild);
                if (mem == null) {
                    return null;
                }

                int minutes = Integer.parseInt(m.group(3));
                return new ParsedAuditReason(mem, minutes, reasonF(m.group(4)));
            }

            // next try the basic pattern
            m = AUDIT_BASIC_PATTERN.matcher(reason);
            if (m.find()) {
                Member mem = OtherUtil.findMember(m.group(1), m.group(2), guild);
                if (mem == null) {
                    return null;
                }

                return new ParsedAuditReason(mem, 0, reasonF(m.group(3)));
            }

            // we got nothin
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public static class ParsedAuditReason {
        public final Member moderator;
        public final int minutes;
        public final String reason;

        private ParsedAuditReason(Member moderator, int minutes, String reason) {
            this.moderator = moderator;
            this.minutes = minutes;
            this.reason = reason;
        }
    }

    // Private methods
    private static String reasonF(String reason) {
        return reason == null || reason.isEmpty() ? NO_REASON : reason;
    }

    private static String limit512(String input) {
        if (input.length() < 512) {
            return input;
        }

        return input.substring(0, 512);
    }
}
