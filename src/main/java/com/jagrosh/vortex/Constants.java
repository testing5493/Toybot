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

import java.time.OffsetDateTime;

/**
 * @author John Grosh (jagrosh)
 */
public class Constants {
    public final static OffsetDateTime STARTUP = OffsetDateTime.now();
    public final static String PREFIX = "?";
    public final static String SUCCESS = ":white_check_mark:";
    public final static String WARNING = ":warning:";
    public final static String ERROR = ":x:";
    public final static String LOADING = ":alarm_clock:";
    public final static String HELP_REACTION = SUCCESS.replaceAll("<a?:(.+):(\\d+)>", "$1:$2");
    public final static String ERROR_REACTION = ERROR.replaceAll("<a?:(.+):(\\d+)>", "$1:$2");

    public final static int DEFAULT_CACHE_SIZE = 8000;
    public final static String OWNER_ID = Vortex.config.getString("owner-id");
    public final static long DELETED_USER_ID = 456226577798135808L;
    public final static long CLYDE_AI_ID = 1081004946872352958L;
    public final static long CLYDE_HOOK_ID = 1;
    public final static long DISCORD_SYSTEM_ID = 643945264868098049L;
    public final static long DISCORD_COMMUNITY_UPDATES_ID = 669627189624307712L;
    public final static long DISCORD_EPOCH = 1420070400000L;
    public final static int SNOWFLAKE_TIME_OFFSET = 22;

    public final static int MAX_NAME_LENGTH = 32;
    public final static String DONATION_LINK = "https://patreon.com/jagrosh";

    public final static class Wiki {
        public final static String PRIMARY_LINK = "https://jagrosh.com/vortex";

        public final static String WIKI_BASE = "https://github.com/jagrosh/Vortex/wiki";
        public final static String START = WIKI_BASE + "/Getting-Started";
        public final static String RAID_MODE = WIKI_BASE + "/Raid-Mode";
        public final static String COMMANDS = WIKI_BASE + "/Commands";
        public final static String AUTOMOD = WIKI_BASE + "/Auto-Moderation";
    }

    private Constants() {}
}
