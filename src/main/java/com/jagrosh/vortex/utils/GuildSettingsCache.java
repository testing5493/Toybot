package com.jagrosh.vortex.utils;

import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.database.managers.GuildSettingsDataManager;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;

/**
 * Just to make lazy loading easier
 */
// TODO: use a dummy GuildSettings object instead
@RequiredArgsConstructor
public class GuildSettingsCache {
    private final Vortex vortex;
    private final Guild g;
    private GuildSettingsDataManager.GuildSettings guildSettings;

    public GuildSettingsDataManager.GuildSettings get() {
        if (guildSettings == null) {
            guildSettings = vortex.getDatabase().settings.getSettings(g);
        }

        return guildSettings;
    }

    public long getGraveledRoleId() {
        return get().getGravelRole(g).getIdLong();
    }

    public long getMutedRoleId() {
        return get().getMutedRole(g).getIdLong();
    }

    public Role getGravelRole() {
        return get().getGravelRole(g);
    }

    public Role getMutedRole() {
        return get().getMutedRole(g);
    }
}
