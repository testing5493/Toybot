package com.jagrosh.vortex.hibernate.entities;

import com.jagrosh.vortex.Constants;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// TODO: Better configure cache
@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Data
@NoArgsConstructor
public class GuildData {
    public static final int PREFIX_MAX_LENGTH = 40;
    public static final int RAIDMODE_INACTIVE = -2;

    @Id
    @Column(name = "GUILD_ID")
    private long guildId;

    @Column(name = "PREFIX", length = PREFIX_MAX_LENGTH)
    private String prefix = Constants.PREFIX;

    @Column(name = "MOD_ROLE_ID")
    private long modRoleId;

    @Column(name = "ADMIN_ROLE_ID")
    private long adminRoleId;

    @Column(name = "RTC_ROLE_ID")
    private long rtcRoleId;

    @Column(name = "CHANNEL_MEMBER_ROLE_ID")
    private long channelMemberRoleId;

    @Column(name = "GRAVEL_ROLE_ID")
    private long gravelRoleId;

    @Column(name = "MUTED_ROLE_ID")
    private long mutedRoleId;

    @Column(name = "MODLOGS_CHANNEL_ID")
    private long modlogsChannelId;

    @Column(name = "RAIDMODE")
    private int raidmode = RAIDMODE_INACTIVE;

    @Nullable
    public Role getModRole(@NotNull Guild g) {
        return g.getRoleById(modRoleId);
    }

    @Nullable
    public Role getAdminRole(@NotNull Guild g) {
        return g.getRoleById(adminRoleId);
    }

    @Nullable
    public Role getRtcRole(@NotNull Guild g) {
        return g.getRoleById(rtcRoleId);
    }

    @Nullable
    Role getChannelMemberRole(@NotNull Guild g) {
        return g.getRoleById(channelMemberRoleId);
    }

    @Nullable
    public Role getGravelRole(@NotNull Guild g) {
        return g.getRoleById(gravelRoleId);
    }

    @Nullable
    public Role getMutedRole(@NotNull Guild g) {
        return g.getRoleById(mutedRoleId);
    }

    @Nullable
    public TextChannel getModlogsChannel(@NotNull Guild g) {
        return g.getTextChannelById(modlogsChannelId);
    }

    public boolean isInRaidMode() {
        return raidmode != RAIDMODE_INACTIVE;
    }
}
