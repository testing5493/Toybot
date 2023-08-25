/*
 * Copyright 2018 John Grosh (jagrosh).
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
package com.jagrosh.vortex;

import com.jagrosh.vortex.hibernate.entities.BanLog;
import com.jagrosh.vortex.hibernate.entities.GravelLog;
import com.jagrosh.vortex.hibernate.entities.KickLog;
import com.jagrosh.vortex.hibernate.entities.ModLog;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author John Grosh (jagrosh)
 */
@AllArgsConstructor
public enum Action {
    GRAVEL("graveled", "gravel", "graveling", Emoji.LOGS.GRAVEL),
    TEMPGRAVEL("tempgraveled", "gravel", "graveling", Emoji.LOGS.GRAVEL),
    UNGRAVEL("ungraveled", "ungravel", "ungraveling", Emoji.LOGS.UNGRAVEL),
    UNMUTE("unmuted", "unmute", "unmuting", Emoji.LOGS.UNMUTE),
    UNBAN("unbanned", "unban", "unbanning", Emoji.LOGS.UNBAN),
    BAN("banned", "ban", "banning", Emoji.LOGS.BAN),
    TEMPBAN("tempbanned", "ban", "tempbanned", Emoji.LOGS.BAN),
    SOFTBAN("softbanned", "softban", "softbanning", Emoji.LOGS.BAN),
    KICK("kicked", "kick", "kicking", Emoji.LOGS.KICK),
    MUTE("muted", "mute", "muting", Emoji.LOGS.MUTE),
    TEMPMUTE("tempmuted", "tempmute", "muting", Emoji.LOGS.MUTE),
    WARN("warned", "warn", "warning", Emoji.LOGS.MODERATION),
    VOICE_KICK("kicked", "kics", "kicking", Emoji.LOGS.DISCONNECT),
    CLEAN("cleaned", "clean", "cleaning", Emoji.LOGS.PURGE),
    DELETE("deleted", "delete", "deleting", Emoji.LOGS.DELETE);

    private final @Getter String pastVerb, verb, presentVerb;
    private final @Getter Emoji.LogEmoji emoji;
}
