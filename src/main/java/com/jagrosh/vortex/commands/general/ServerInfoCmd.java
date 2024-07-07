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

import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.commands.HybridEvent;
import com.jagrosh.vortex.utils.FormatUtil;
import com.jagrosh.vortex.utils.ToycatPallete;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.utils.TimeFormat;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

/**
 * @author John Grosh (jagrosh)
 */
@Slf4j
public class ServerInfoCmd extends GeneralHybridCmd {
    private final static String LINESTART = "\u25AB"; // ▫
    private final static String GUILD_EMOJI = "\uD83D\uDDA5"; // 🖥
    private final static String NO_REGION = "\u2754"; // ❔

    public ServerInfoCmd(Vortex vortex) {
        super(vortex);
        this.aliases = new String[]{"server", "guildinfo"};
        this.name = "serverinfo";
        this.help = "shows server info";
        this.guildOnly = true;
    }

    @Override
    protected void execute(HybridEvent e) {
        Guild g = e.getGuild();
        Guild.MetaData metaData = null;
        try {
            metaData = g.retrieveMetaData().complete();
        } catch (Throwable t) {
            log.error("Failed to load metadata for guild {}", g.getId(), t);
        }

        long botCount = g.getMemberCache().stream().filter(m -> m.getUser().isBot()).count();
        long onlineCount = metaData != null ? metaData.getApproximatePresences() : g.getMemberCache()
                                                                                    .stream()
                                                                                    .filter((u) -> u.getOnlineStatus() != OnlineStatus.OFFLINE)
                                                                                    .count();

        String verif = switch (g.getVerificationLevel()) {
            case VERY_HIGH -> "┻━┻ミヽ(ಠ益ಠ)ノ彡┻━┻";
            case HIGH -> "(╯°□°）╯︵ ┻━┻";
            default -> FormatUtil.capitalize(g.getVerificationLevel().toString()).trim();
        };

        EmbedBuilder builder = new EmbedBuilder()
                .setColor(ToycatPallete.LIGHT_BROWN)
                .setTitle("Showing Info For " + g.getName())
                .setThumbnail(g.getIconUrl())
                .addField("ID", g.getId(), true)
                .addField("Owner", g.getOwner() == null ? "Unknown" : g.getOwner().getAsMention(), true)
                .addField("Created At", TimeFormat.DATE_SHORT.format(g.getTimeCreated()), true)
                .addField("Cached Users", String.format("%d (%d online, %d bots)", g.getMemberCount(), onlineCount, botCount), true)
                .addField("Verification", verif, true).addField("Roles", "" + g.getRoles().size(), true);

        if (g.getBoostRole() != null) {
            int boosters = g.getBoosters().size();
            int boosts = g.getBoostCount();
            String boosterExtendedInfo = boosts == 0 ? "(0 boosts)" : String.format("(%d booster%s, %d boost%s)", boosters, boosters == 1 ? "" : "s", boosts, boosts == 1 ? "" : "s");
            builder.addField("Booster Role", String.format("%s (%s)", g.getBoostRole().getAsMention(), boosterExtendedInfo), true);
        }

        if (g.getRulesChannel() != null) {
            builder.addField("Rules Channel", g.getRulesChannel().getAsMention(), true);
        }

        if (g.getVanityCode() != null) {
            builder.addField("Vanity Invite", String.format("[discord.gg/%s](%s)", g.getVanityCode(), g.getVanityUrl()), true);
        }

        FormatUtil.IconURLFieldBuilder urlBuilder = new FormatUtil.IconURLFieldBuilder().add("Icon", g.getIconUrl())
                .add("Banner", g.getBannerUrl())
                .add("Invite Splash", g.getSplashUrl());
        if (!urlBuilder.isEmpty()) {
            builder.addField("Images", urlBuilder.toString(), true);
        }

        e.reply(MessageCreateData.fromEmbeds(builder.build()));
    }

    // Old one kept for reference
    @Deprecated
    private MessageCreateData getServerInfoEmbed(Guild guild) {
        long onlineCount = guild.getMembers().stream().filter((u) -> (u.getOnlineStatus() != OnlineStatus.OFFLINE)).count();
        long botCount = guild.getMembers().stream().filter(m -> m.getUser().isBot()).count();
        String verif = switch (guild.getVerificationLevel()) {
            case VERY_HIGH -> "┻━┻ミヽ(ಠ益ಠ)ノ彡┻━┻";
            case HIGH -> "(╯°□°）╯︵ ┻━┻";
            default -> FormatUtil.capitalize(guild.getVerificationLevel().toString()).trim();
        };

        EmbedBuilder builder = new EmbedBuilder().setColor(ToycatPallete.LIGHT_BROWN).setTitle("Showing Info For " + guild.getName()).setThumbnail(guild.getIconUrl()).addField("ID", guild.getId(), true).addField("Owner", guild.getOwner() == null ? "Unkown" : guild.getOwner().getAsMention(), true).addField("Created At", TimeFormat.DATE_SHORT.format(guild.getTimeCreated()), true).addField("Cached Users", String.format("%d (%d online, %d bots)", guild.getMemberCount(), onlineCount, botCount), true).addField("Verification", verif, true).addField("Roles", "" + guild.getRoles().size(), true);

        if (guild.getBoostRole() != null) {
            int boosters = guild.getBoosters().size();
            int boosts = guild.getBoostCount();
            String boosterExtendedInfo = boosts == 0 ? "(0 boosts)" : String.format("(%d booster%s, %d boost%s)", boosters, boosters == 1 ? "" : "s", boosts, boosts == 1 ? "" : "s");
            builder.addField("Booster Role", String.format("%s (%s)", guild.getBoostRole().getAsMention(), boosterExtendedInfo), true);
        }

        if (guild.getRulesChannel() != null) {
            builder.addField("Rules Channel", guild.getRulesChannel().getAsMention(), true);
        }

        FormatUtil.IconURLFieldBuilder urlBuilder = new FormatUtil.IconURLFieldBuilder().add("Icon", guild.getIconUrl()).add("Banner", guild.getBannerUrl()).add("Invite Splash", guild.getSplashUrl());
        if (!urlBuilder.isEmpty()) {
            builder.addField("Images", urlBuilder.toString(), true);
        }

        return MessageCreateData.fromEmbeds(builder.build());
    }
}
