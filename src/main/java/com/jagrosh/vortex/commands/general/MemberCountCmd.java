package com.jagrosh.vortex.commands.general;

import com.jagrosh.vortex.Emoji;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.commands.HybridEvent;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Invite;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.attribute.IInviteContainer;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.internal.entities.channel.concrete.CategoryImpl;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
public class MemberCountCmd extends GeneralHybridCmd {
    public MemberCountCmd(Vortex vortex) {
        super(vortex);
        this.guildOnly = true;
        this.name = "membercount";
        this.help = "Shows the amount of members in this server";
        this.aliases = new String[]{"count"};
    }

    protected void execute(HybridEvent e) {
        Guild g = e.getGuild();
        Member self = g.getSelfMember();
        Invite.Guild inviteGuild = null;

        if (g.getVanityCode() != null) {
            inviteGuild = getInviteGuild(g, g.getVanityCode());
        }

        if (inviteGuild == null && self.hasPermission(Permission.MANAGE_SERVER)) {
            List<Invite> inviteList = g.retrieveInvites().complete();
            if (!inviteList.isEmpty()) {
                inviteGuild = getInviteGuild(g, inviteList.get(0).getCode());
            }
        }

        if (inviteGuild == null) {
            for (GuildChannel channel : g.getChannels()) {
                if (channel instanceof IInviteContainer invChannel) {
                    if (self.hasPermission(channel, Permission.CREATE_INSTANT_INVITE)) {
                        try {
                            Invite invite = invChannel.createInvite().setMaxAge(1L, TimeUnit.MINUTES).reason("Checking server member count").complete();
                            inviteGuild = getInviteGuild(g, invite.getCode());
                            break;
                        } catch (Exception ex) {
                            log.warn("Exception occurred retrieving member count", ex);
                            break;
                        }
                    }
                }
            }
        }

        if (inviteGuild != null && inviteGuild.getMemberCount() != -1 && inviteGuild.getOnlineCount() != -1) {
            e.reply(String.format("%d (%s %d)", inviteGuild.getMemberCount(), Emoji.STATUS_ONLINE, inviteGuild.getOnlineCount()));
        } else {
            if (g.getMemberCount() < 2) {
                e.reply("Please give the bot Manage Server or Create Invite perms to use this command");
            }

            e.reply(String.format("~%d", g.getMemberCount()));
        }
    }

    private Invite.Guild getInviteGuild(Guild g, String invite) {
        try {
            Invite.Guild invGuild = Invite.resolve(g.getJDA(), invite, true)
                    .complete()
                    .getGuild();

            if (invGuild.getMemberCount() == -1 || invGuild.getOnlineCount() == -1) {
                return null;
            } else {
                return invGuild;
            }
        } catch (Exception e) {
            return null;
        }
    }
}
