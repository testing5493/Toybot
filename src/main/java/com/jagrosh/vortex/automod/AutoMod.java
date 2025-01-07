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
package com.jagrosh.vortex.automod;

import com.jagrosh.vortex.Constants;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.automod.URLResolver.ActiveURLResolver;
import com.jagrosh.vortex.automod.URLResolver.DummyURLResolver;
import com.jagrosh.vortex.database.managers.AutomodManager.AutomodSettings;
import com.jagrosh.vortex.hibernate.api.ModlogManager;
import com.jagrosh.vortex.hibernate.entities.*;
import com.jagrosh.vortex.logging.MessageCache.CachedMessage;
import com.jagrosh.vortex.utils.FixedCache;
import com.jagrosh.vortex.utils.FormatUtil;
import com.jagrosh.vortex.utils.OtherUtil;
import com.typesafe.config.Config;
import jakarta.persistence.PersistenceException;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Guild.VerificationLevel;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.PermissionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author John Grosh (jagrosh)
 */
@Slf4j
public class AutoMod {
    private static final Pattern INVITES = Pattern.compile("discord\\s?(?:(?:\\.|dot|\\(\\.\\)|\\(dot\\))\\s?gg|(?:app)?\\s?\\.\\s?com\\s?/\\s?invite)\\s?/\\s?([A-Z0-9-]{2,18})", Pattern.CASE_INSENSITIVE);
    private static final Pattern REF = Pattern.compile("https?://\\S+(?:/ref/|[?&#]ref(?:errer|erral)?=)\\S+", Pattern.CASE_INSENSITIVE);
    private static final Pattern BASE_URL = Pattern.compile("https?://(?:[^?&:/\\s]+\\.)?([^?&:/\\s]+\\.\\w+)(?:\\W|$)", Pattern.CASE_INSENSITIVE);
    private static final Pattern LINK = Pattern.compile("https?://\\S+", Pattern.CASE_INSENSITIVE);
    private static final String INVITE_LINK = "https?://discord(?:app\\.com/invite|\\.com/invite|\\.gg)/(\\S+)";
    private static final String CONDENSER = "(.+?)\\s*(\\1\\s*)+";
    private static final Logger LOG = LoggerFactory.getLogger("AutoMod");
    public static final String RESTORE_MUTE_ROLE_AUDIT = "Restoring Muted Role";
    public static final String RESTORE_GRAVEL_ROLE_AUDIT = "Restoring Gravel Role";

    private final Vortex vortex;

    private String[] refLinkList;
    private final URLResolver urlResolver;
    private final InviteResolver inviteResolver = new InviteResolver();
    private final CopypastaResolver copypastaResolver = new CopypastaResolver();
    private final FixedCache<String, DupeStatus> spams = new FixedCache<>(3000);
    private final HashMap<Long, OffsetDateTime> latestGuildJoin = new HashMap<>();

    public AutoMod(Vortex vortex, Config config) {
        this.vortex = vortex;
        this.urlResolver = config.getBoolean("url-resolver.active") ? new ActiveURLResolver(config) : new DummyURLResolver();
        loadCopypastas();
        loadReferralDomains();
    }

    public final void loadCopypastas() {
        this.copypastaResolver.load();
    }

    public final void loadSafeDomains() {
        this.urlResolver.loadSafeDomains();
    }

    public final void loadReferralDomains() {
        this.refLinkList = OtherUtil.readLines("referral_domains");
    }

    public void enableRaidMode(Guild guild, Member moderator, OffsetDateTime now, String reason) {
        GuildData guildData = vortex.getHibernate().guild_data.getGuildData(guild.getIdLong());
        guildData.setRaidmode(guild.getVerificationLevel().getKey());
        vortex.getHibernate().guild_data.updateGuildData(guildData);

        if (guild.getVerificationLevel().getKey() < VerificationLevel.HIGH.getKey()) {
            try {
                guild.getManager().setVerificationLevel(VerificationLevel.HIGH).reason("Enabling Anti-Raid Mode").queue();
            } catch (PermissionException ignore) {
            }
        }

        // vortex.getModLogger().postRaidmodeCase(moderator, now, true, reason);
    }

    public void disableRaidMode(Guild guild, Member moderator, OffsetDateTime now, String reason) {
        // TODO: See if you would need to disable raid mode in the database
        GuildData guildData = vortex.getHibernate().guild_data.getGuildData(guild.getIdLong());
        Guild.VerificationLevel last = Guild.VerificationLevel.fromKey(guildData.getRaidmode());
        if (guild.getVerificationLevel() != last) {
            try {
                guild.getManager().setVerificationLevel(last).reason("Disabling Anti-Raid Mode").queue();
            } catch (PermissionException ignore) {
            }
        }

        // vortex.getModLogger().postRaidmodeCase(moderator, now, false, reason);
    }

    private TimedLog autoPardon(TimedLog timedLog) {
        try {
            timedLog.setPardoningTime(Instant.now());
            timedLog.setPardoningModId(ModlogManager.AUTOMOD_ID);
            return switch (timedLog) {
                case GravelLog gravelLog -> vortex.getHibernate().modlogs.logUngravel(gravelLog);
                case MuteLog muteLog -> vortex.getHibernate().modlogs.logUnmute(muteLog);
                case BanLog banLog -> vortex.getHibernate().modlogs.logUnban(banLog);
                default -> throw new UnsupportedOperationException();
            };
        } catch (PersistenceException e) {
            return null;
        }
    }

    public void checkAutoPardons() {
        JDA jda = vortex.getJda();
        for (TimedLog timedLog : vortex.getHibernate().modlogs.checkAutoPardons()) {
            System.out.println(timedLog);
            Guild g = jda.getGuildById(timedLog.getGuildId());
            if (g == null || jda.isUnavailable(g.getIdLong())) {
                continue;
            }

            Member m = OtherUtil.getMemberCacheElseRetrieve(g, timedLog.getUserId());

            if (timedLog instanceof BanLog banLog) {
                if (!g.getSelfMember().hasPermission(Permission.BAN_MEMBERS)) {
                    continue;
                }

                if (m != null) {
                    banLog.setPardoningTime(Instant.now());
                    banLog.setPardoningModId(ModlogManager.UNKNOWN_MOD_ID);
                    try {
                        vortex.getHibernate().modlogs.logUnban(banLog);
                    } catch (Exception ignore) {}

                    continue;
                }

                g.unban(User.fromId(timedLog.getUserId())).reason("Temporary Ban Completed").queue(s -> {
                    autoPardon(timedLog);
                }, f -> {
                    if (f instanceof ErrorResponseException err) {
                        switch (err.getErrorResponse()) {
                            case UNKNOWN_USER -> autoPardon(timedLog);
                            case null, default -> log.warn("Failed to unban " + timedLog + " due to " + err.getErrorResponse());
                        }
                    } else {
                        log.warn("Failed to unban " + timedLog, f);
                    }
                });
            } else if (timedLog instanceof GravelLog gravelLog || timedLog instanceof MuteLog muteLog) {
                boolean isGravel = timedLog instanceof GravelLog;

                GuildData guildData = vortex.getHibernate().guild_data.getGuildData(g.getIdLong());
                Role role = isGravel ? guildData.getGravelRole(g) : guildData.getMutedRole(g);
                if (role == null) {
                    autoPardon(timedLog);
                    continue;
                }

                if (m != null) {
                    g.removeRoleFromMember(m, role)
                            .reason(FormatUtil.capitalize(timedLog.actionType().getVerb()) + " finished")
                            .queue(s -> autoPardon(timedLog), f -> {
                                if (f instanceof ErrorResponseException err) {
                                    switch (err.getErrorResponse()) {
                                        case UNKNOWN_MEMBER, UNKNOWN_ROLE -> autoPardon(timedLog);
                                        case MISSING_PERMISSIONS -> {}
                                        case null, default -> log.warn("Failed to un" + timedLog.actionType().getVerb() + " " + timedLog + " due to " + err.getErrorResponse());
                                    }
                                } else {
                                    log.warn("Failed to unban " + timedLog, f);
                                }
                            });
                } else {
                    autoPardon(timedLog);
                }
            }
        }
    }



    public void memberJoin(GuildMemberJoinEvent event) {
        // completely ignore bots for raidmode
        if (event.getMember().getUser().isBot()) {
            return;
        }

        GuildData guildData = vortex.getHibernate().guild_data.getGuildData(event.getGuild().getIdLong());
        boolean inRaidMode = guildData.isInRaidMode();
        AutomodSettings ams = vortex.getDatabase().automod.getSettings(event.getGuild());
        OffsetDateTime now = OffsetDateTime.now();
        boolean kicking = false;

        // if we're in raid mode...
        if (inRaidMode) {
            // ...and this server uses auto raid mode, check if we should be turning it off automatically
            // this means that we should turn it off if the latest attempted join was more than 2 minutes ago
            if (ams.useAutoRaidMode() && latestGuildJoin.containsKey(event.getGuild().getIdLong()) && latestGuildJoin.get(event.getGuild().getIdLong()).until(now, ChronoUnit.SECONDS) > 120) {
                disableRaidMode(event.getGuild(), event.getGuild().getSelfMember(), now, "No recent join attempts");
            }
            // otherwise, boot 'em
            else if (event.getGuild().getSelfMember().hasPermission(Permission.KICK_MEMBERS)) {
                kicking = true;
            }
        } else if (ams.useAutoRaidMode()) { // now, if we're not in raid mode, and auto mode is enabled
            // find the time that we should be looking after, and count the number of people that joined after that
            OffsetDateTime min = event.getMember().getTimeJoined().minusSeconds(ams.raidmodeTime);
            long recent = event.getGuild().getMemberCache().stream().filter(m -> !m.getUser().isBot() && m.getTimeJoined().isAfter(min)).count();
            if (recent >= ams.raidmodeNumber) {
                enableRaidMode(event.getGuild(), event.getGuild().getSelfMember(), now, "Maximum join rate exceeded (" + ams.raidmodeNumber + "/" + ams.raidmodeTime + "s)");
                kicking = true;
            }
        }

        if (kicking) {
            OtherUtil.safeDM(event.getUser(), "Sorry, **" + event.getGuild().getName() + "** is currently under lockdown. " + "Please try joining again later. Sorry for the inconvenience.", true, () -> {
                try {
                    event.getGuild().kick(event.getUser()).reason("Anti-Raid Mode").queue();
                } catch (Exception ignore) {
                }
            });
        } else {
            List<TimedLog> timedLogs = vortex.getHibernate().modlogs.getCurrentPunishments(event.getGuild().getIdLong(), event.getMember().getIdLong());
            for (TimedLog timedLog : timedLogs) {
                try {
                    if (timedLog instanceof MuteLog) {
                        event.getGuild().addRoleToMember(event.getMember(), guildData.getMutedRole(event.getGuild())).reason(RESTORE_MUTE_ROLE_AUDIT).queue();
                    } else if (timedLog instanceof GravelLog) {
                        event.getGuild().addRoleToMember(event.getMember(), guildData.getGravelRole(event.getGuild())).reason(RESTORE_GRAVEL_ROLE_AUDIT).queue();

                    }
                } catch (Exception ignore) {}
            }

            dehoist(event.getMember());
        }

        latestGuildJoin.put(event.getGuild().getIdLong(), now);
    }


    private boolean shouldPerformAutomod(Member member, TextChannel channel) {
        // ignore users not in the guild
        if (member == null) {
            return false;
        }

        // ignore broken guilds
        if (member.getGuild().getOwner() == null) {
            return false;
        }

        // ignore bots
        if (member.getUser().isBot()) {
            return false;
        }

        // ignore users vortex cant interact with
        if (!member.getGuild().getSelfMember().canInteract(member)) {
            return false;
        }

        // ignore users that can kick, ban, or manage server
        if (member.hasPermission(Permission.KICK_MEMBERS) || member.hasPermission(Permission.BAN_MEMBERS) || member.hasPermission(Permission.MANAGE_SERVER)) {
            return false;
        }

        // if a channel is specified, ignore users that can manage messages in that channel
        if (channel != null && (member.hasPermission(channel, Permission.MESSAGE_MANAGE) || vortex.getDatabase().ignores.isIgnored(channel))) {
            return false;
        }

        return !vortex.getDatabase().ignores.isIgnored(member);
    }

    public void dehoist(Member member) {
        if (!member.getGuild().getSelfMember().hasPermission(Permission.NICKNAME_MANAGE)) {
            return;
        }

        if (!shouldPerformAutomod(member, null)) {
            return;
        }

        AutomodSettings settings = vortex.getDatabase().automod.getSettings(member.getGuild());
        if (settings == null || settings.dehoistChar == (char) 0 || member.getEffectiveName().charAt(0) > settings.dehoistChar) {
            return;
        }

        try {
            OtherUtil.dehoist(member, settings.dehoistChar);
        } catch (Exception ignore) {
        }
    }

    public void performAutomod(Message message) {
        if (true) { // TODO: Implement
            return;
        }

        //ignore users with Manage Messages, Kick Members, Ban Members, Manage Server, or anyone the bot can't interact with
        if (!shouldPerformAutomod(message.getMember(), message.getChannel().asTextChannel())) {
            return;
        }

        //get the settings
        AutomodSettings settings = vortex.getDatabase().automod.getSettings(message.getGuild());
        if (settings == null) {
            return;
        }

        // check the channel for channel-specific settings
        String topic = message.getChannel().asTextChannel().getTopic();
        boolean preventSpam = topic == null || !topic.toLowerCase().contains("{spam}");
        boolean preventInvites = (topic == null || !topic.toLowerCase().contains("{invites}")) && settings.filterInvites;

        List<Long> inviteWhitelist = !preventInvites ? Collections.emptyList() : vortex.getDatabase().inviteWhitelist.readWhitelist(message.getGuild());

        boolean shouldDelete = false;
        String channelWarning = null;
        StringBuilder reason = new StringBuilder();

        // anti-duplicate
        if (settings.useAntiDuplicate() && preventSpam) {
            String key = message.getAuthor().getId() + "|" + message.getGuild().getId();
            String content = condensedContent(message);
            DupeStatus status = spams.get(key);
            if (status == null) {
                spams.put(key, new DupeStatus(content, latestTime(message)));
            } else {
                OffsetDateTime now = latestTime(message);
                int offenses = status.update(content, now);

                if (offenses == settings.dupeDeleteThresh) {
                    channelWarning = "Please stop spamming.";
                    purgeMessages(message.getGuild(), m -> m.getAuthorId() == message.getAuthor().getIdLong() && m.getTimeCreated().plusMinutes(2).isAfter(now));
                } else if (offenses > settings.dupeDeleteThresh) {
                    shouldDelete = true;
                }
            }
        }

        // max newlines
        if (settings.maxLines > 0 && preventSpam) {
            int count = message.getContentRaw().split("\n").length;
            if (count > settings.maxLines) {
                reason.append(", Message contained ").append(count).append(" newlines");
                shouldDelete = true;
            }
        }

        // filters
        Filter badWordsFilter = vortex.getDatabase().filters.getBadWordsFilter(message.getGuild().getIdLong());
        Filter veryBadWordsFilter = vortex.getDatabase().filters.getVeryBadWordsFilter(message.getGuild().getIdLong());
        if (veryBadWordsFilter != null && veryBadWordsFilter.test(message.getContentRaw())) {
            shouldDelete = true;
            reason.append(", Very Bad Words Filter");
        } else if (badWordsFilter != null && badWordsFilter.test(message.getContentRaw())) {
            shouldDelete = true;
            reason.append(", Bad Words Filter");
        }

        // prevent referral links
        if (settings.filterRefs) {
            Matcher m = REF.matcher(message.getContentRaw());
            if (m.find()) {
                reason.append(", Referral link");
                shouldDelete = true;
            } else {
                m = BASE_URL.matcher(message.getContentRaw().toLowerCase());
                while (m.find()) {
                    if (isReferralUrl(m.group(1))) {
                        reason.append(", Referral link");
                        shouldDelete = true;
                        break;
                    }
                }
            }
        }

        // prevent copypastas
        if (settings.filterCopypastas && preventSpam) {
            String copypastaName = copypastaResolver.getCopypasta(message.getContentRaw());
            if (copypastaName != null) {
                reason.append(", ").append(copypastaName).append(" copypasta");
                shouldDelete = true;
            }
        }

        // anti-invite
        if (preventInvites) {
            List<String> invites = new ArrayList<>();
            Matcher m = INVITES.matcher(message.getContentRaw());
            while (m.find()) {
                invites.add(m.group(1));
            }

            LOG.trace("Found " + invites.size() + " invites.");
            for (String inviteCode : invites) {
                LOG.info("Resolving invite in " + message.getGuild().getId() + ": " + inviteCode);
                long gid = inviteResolver.resolve(inviteCode, message.getJDA());
                if (gid != message.getGuild().getIdLong() && !inviteWhitelist.contains(gid)) {
                    reason.append(", Advertising");
                    shouldDelete = true;
                    break;
                }
            }
        }

        // delete the message if applicable
        if (shouldDelete) {
            try {
                message.delete().reason("Automod").queue(v -> {}, f -> {});
            } catch (PermissionException ignore) {
            }
        }

        // send a short 'warning' message that self-deletes
        if (channelWarning != null && message.getGuild().getSelfMember().hasPermission(message.getChannel().asTextChannel(), Permission.MESSAGE_SEND)) {
            message.getChannel().sendMessage(message.getAuthor().getAsMention() + Constants.WARNING + " " + channelWarning).queue(m -> m.delete().queueAfter(2500, TimeUnit.MILLISECONDS, s -> {}, f -> {}), f -> {});
        }

        // now, lets resolve links, but async
        if (!shouldDelete && settings.resolveUrls && (preventInvites || settings.filterRefs)) {
            List<String> links = new LinkedList<>();
            Matcher m = LINK.matcher(message.getContentRaw());
            while (m.find()) {
                links.add(m.group().endsWith(">") ? m.group().substring(0, m.group().length() - 1) : m.group());
            }

            if (!links.isEmpty()) {
                vortex.getThreadpool().execute(() -> {
                    boolean containsInvite = false;
                    boolean containsRef = false;
                    String llink = null;
                    List<String> redirects = null;
                    for (String link : links) {
                        llink = link;
                        redirects = urlResolver.findRedirects(link);
                        for (String resolved : redirects) {
                            if (preventInvites && resolved.matches(INVITE_LINK)) {
                                String code = resolved.replaceAll(INVITE_LINK, "$1");
                                LOG.info("Delayed resolving invite in " + message.getGuild().getId() + ": " + code);
                                long invite = inviteResolver.resolve(code, message.getJDA());
                                if (invite != message.getGuild().getIdLong() && !inviteWhitelist.contains(invite)) {
                                    containsInvite = true;
                                }
                            }

                            if (settings.filterRefs) {
                                if (resolved.matches(REF.pattern()) || isReferralUrl(resolved.replaceAll(BASE_URL.pattern(), "$1"))) {
                                    containsRef = true;
                                }
                            }
                        }

                        if ((containsInvite || !preventInvites) && (containsRef || settings.filterRefs)) {
                            break;
                        }
                    }

                    if (containsInvite || containsRef) {
                        vortex.getBasicLogger().logRedirectPath(message, llink, redirects);
                        String rreason = ((containsInvite ? ", Advertising (Resolved Link)" : "") + (containsRef ? ", Referral Link (Resolved Link)" : "")).substring(2);
                        try {
                            message.delete().reason(rreason).queue(v -> {}, f -> {});
                        } catch (PermissionException ignore) {
                        }
                    }
                });
            }
        }
    }

    private void purgeMessages(Guild guild, Predicate<CachedMessage> predicate) {
        vortex.getMessageCache().getMessages(guild, predicate).stream().collect(Collectors.groupingBy(CachedMessage::getTextChannelId)).entrySet().forEach(entry -> {
            try {
                TextChannel mtc = guild.getTextChannelById(entry.getKey());
                if (mtc != null) {
                    mtc.purgeMessagesById(entry.getValue().stream().map(CachedMessage::getId).collect(Collectors.toList()));
                }
            } catch (PermissionException ignore) {
            } catch (Exception ex) {
                LOG.error("Error in purging messages: ", ex);
            }
        });
    }

    private boolean isReferralUrl(String url) {
        // TODO: Make it search against a presorted refferal list?
        for (String reflink : refLinkList) {
            if (reflink.equalsIgnoreCase(url)) {
                return true;
            }
        }

        return false;
    }

    private final static List<String> ZEROWIDTH = Arrays.asList("\u00AD", "\u034F", "\u17B4", "\u17B5", "\u180B", "\u180C", "\u180D", "\u180E", "\u200B", "\u200C", "\u200D", "\u200E", "\u202A", "\u202C", "\u202D", "\u2060", "\u2061", "\u2062", "\u2063", "\u2064", "\u2065", "\u2066", "\u2067", "\u2068", "\u2069", "\u206A", "\u206B", "\u206C", "\u206D", "\u206E", "\u206F", "\uFE00", "\uFE01", "\uFE02", "\uFE03", "\uFE04", "\uFE05", "\uFE06", "\uFE07", "\uFE08", "\uFE09", "\uFE0A", "\uFE0B", "\uFE0C", "\uFE0D", "\uFE0E", "\uFE0F", "\uFEFF", "\uFFF0", "\uFFF1", "\uFFF2", "\uFFF3", "\uFFF4", "\uFFF5", "\uFFF6", "\uFFF7", "\uFFF8");

    private static String condensedContent(Message m) {
        StringBuilder sb = new StringBuilder(m.getContentRaw());
        m.getAttachments().forEach(at -> sb.append("\n").append(at.getFileName()));
        try {
            StringBuilder sb2 = new StringBuilder();
            sb.toString().chars().filter(c -> !ZEROWIDTH.contains(Character.toString((char) c))).forEach(c -> sb2.append((char) c));
            return sb2.toString().trim().replaceAll(CONDENSER, "$1");
        } catch (Exception ex) {
            return sb.toString().trim();
        }
    }

    private static OffsetDateTime latestTime(Message m) {
        return m.isEdited() ? m.getTimeEdited() : m.getTimeCreated();
    }

    private static class DupeStatus {
        private String content;
        private OffsetDateTime time;
        private int count;

        private DupeStatus(String content, OffsetDateTime time) {
            this.content = content;
            this.time = time;
            count = 0;
        }

        private int update(String nextcontent, OffsetDateTime nexttime) {
            if (nextcontent.equals(content) && this.time.plusSeconds(30).isAfter(nexttime)) {
                count++;
                this.time = nexttime;
            } else {
                this.content = nextcontent;
                this.time = nexttime;
                count = 0;
            }

            return count;
        }
    }
}
