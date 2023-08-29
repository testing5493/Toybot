package com.jagrosh.vortex.hibernate.api;

import com.jagrosh.vortex.hibernate.entities.GuildData;

public class GuildDataManager {
    private final Database database;

    public GuildDataManager(Database database) {
        this.database = database;
    }

    public GuildData getGuildData(long guildId) {
        return database.doTransaction(session -> {
            GuildData data = session.get(GuildData.class, guildId);
            if (data == null) {
                data = new GuildData();
                data.setGuildId(guildId);
            }

            return data;
        });
    }

    public void updateGuildData(GuildData guildData) {
        database.doTransaction(session -> {
            session.merge(guildData);
        });
    }
}
