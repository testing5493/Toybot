package com.jagrosh.vortex.utils;

import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.database.managers.GuildSettingsDataManager;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.entities.Guild;

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
        return guildSettings != null ? guildSettings : vortex.getDatabase().settings.getSettings(g);
    }

    public long getGraveledRoleId() {
        return get().getGravelRoleId();
    }

    public long getMutedRoleId() {
        return get().getMutedRoleId();
    }
}
