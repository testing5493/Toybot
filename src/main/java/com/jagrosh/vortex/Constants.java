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

import com.typesafe.config.ConfigException;
import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;

/**
 * @see Emoji
 * @author John Grosh (jagrosh)
 */
@Slf4j
public class Constants {
    public static final OffsetDateTime STARTUP = OffsetDateTime.now();
    public static final String CONFIG_FILE_NAME = "application.conf";
    public static final String PREFIX = "?";
    public static final String SUCCESS = ":white_check_mark:";
    public static final char   ZWSP = '\u200B';
    public static final String WARNING = ":warning:";
    public static final String ERROR = ":x:";
    public static final String LOADING = ":alarm_clock:";
    public static final String HELP_REACTION = SUCCESS.replaceAll("<a?:(.+):(\\d+)>", "$1:$2");
    public static final String ERROR_REACTION = ERROR.replaceAll("<a?:(.+):(\\d+)>", "$1:$2");

    public static final int  DEFAULT_CACHE_SIZE = 8000;
    public static final long DELETED_USER_ID = 456226577798135808L;
    public static final long CLYDE_AI_ID = 1081004946872352958L;
    public static final long CLYDE_HOOK_ID = 1;
    public static final long DISCORD_SYSTEM_ID = 643945264868098049L;
    public static final long DISCORD_COMMUNITY_UPDATES_ID = 669627189624307712L;
    public static final long DISCORD_EPOCH = 1420070400000L;
    public static final int  SNOWFLAKE_TIME_OFFSET = 22;
    public static final int DEFAULT_MAX_INLINE = 32;

    public static final String OWNER_ID = checkAndGetOwnerId();

    public static final int MAX_NAME_LENGTH = 32;
    public static final String DONATION_LINK = "https://patreon.com/jagrosh";

    public static final class Wiki {
        public static final String PRIMARY_LINK = "https://jagrosh.com/vortex";
        public static final String WIKI_BASE = "https://github.com/jagrosh/Vortex/wiki";
        public static final String START = WIKI_BASE + "/Getting-Started";
        public static final String RAID_MODE = WIKI_BASE + "/Raid-Mode";
        public static final String COMMANDS = WIKI_BASE + "/Commands";
        public static final String AUTOMOD = WIKI_BASE + "/Auto-Moderation";
    }

    private Constants() {}

    private static String checkAndGetOwnerId() {
        String ownerId;
        try {
            ownerId = Vortex.config.getString("owner-id");
            if (0L >= Long.parseLong(ownerId)) {
                ownerId = null;
            }
        } catch (Exception e) {
            ownerId = null;
        }

        if (ownerId == null) {
            log.warn("A valid owner ID was not found in {}. Owner only commands will not work", CONFIG_FILE_NAME);
            return "0";
        } else {
            return ownerId;
        }
    }
}
