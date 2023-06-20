/*
 * Copyright 2021 John Grosh (john.a.grosh@gmail.com).
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
 * limitations under the License. Furthermore, I'm putting this sentence in all files because I messed up git and its not showing files as edited -\\_( :) )_/-
 */
package com.jagrosh.vortex;

import lombok.AllArgsConstructor;

/**
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class Emoji {
    // vortex general icons
    public final static String TOYBOT  = "<:vortex:850113634859483176>";
    public final static String SUCCESS = "<:vSuccess:850102850491121725>";
    public final static String WARNING = "<:vWarning:850102850855895110>";
    public final static String ERROR   = "<:vError:850102850846457879>";
    public final static String LOADING = "<a:typing:850124618932944937>";

    // badges
    public final static String ACTIVE_DEVELOPER     = "<:active_developer:1109963459929587713>";
    public final static String BOT                  = "<:bot_1:1117663424873246810><:bot_2:1117663423631728670>";
    public final static String VERIFIED_BOT         = "<:verified_bot_1:1117663419961720832><:verified_bot_2:1117663426534182972><:verified_bot_3:1117663422432157837>";
    public final static String VERIFIED_EARLY_DEV   = "<:verified_developer:1109962363316215860>";
    public final static String BUG_HUNTER_LEVEL_1   = "<:bug_hunter1:1109963458356727851>";
    public final static String BUG_HUNTER_LEVEL_2   = "<:bug_hunter2:1109963456414756915>";
    public final static String DISCORD_STAFF        = "<:discord_staff:1109962360887722066>";
    public final static String EARLY_NITRO_SUB      = "<:early_nitro_supporter:1109965926826266694>";
    public final static String HYPESQUAD_BALANCE    = "<:hypesquad_balance:1109965531542470756>";
    public final static String HYPESQUAD_BRAVERY    = "<:hypesquad_balance:1109965531542470756>";
    public final static String HYPESQUAD_BRILIANCE  = "<:hypesquad_brilliance:1109962357788115008>";
    public final static String HYPESQUAD_EVENTS     = "<:hypesquad_events:1112537171090489388>";
    public final static String MODERATOR_ALUMNI     = "<:moderator_alumni:1109966127842480172>";
    public final static String NITRO                = "<:nitro:1109965928113897583>";
    public final static String PARTNERED_USER       = "<:partner:1109962367795732480>";
    public final static String PARTNERED_SERVER     = "<:partnered_server:1109962365610512476>";
    public final static String SERVER_BOOSTER       = "<:server_booster:1109965535019540582>";
    public final static String SERVER_OWNER         = "<:server_owner:1109965533698347099>";
    public final static String NEW_MEMBER           = "<:new_user:1112503015199473746>";
    public final static String DESKTOP_ONLINE       = "<:desktop_online:1112512720051380244>";
    public final static String DESKTOP_IDLE         = "<:desktop_idle:1112512761549819915>";
    public final static String DESKTOP_DND          = "<:desktop_dnd:1112512797587279922>";
    public final static String DESKTOP_OFFLINE      = "<:desktop_offline:1112512846966820925>";
    public final static String MOBILE_ONLINE        = "<:mobile_online:1112513069705334836>";
    public final static String MOBILE_IDLE          = "<:mobile_idle:1112513110356529242>";
    public final static String MOBILE_DND           = "<:mobile_dnd:1112513142367473754>";
    public final static String MOBILE_OFFLINE       = "<:mobile_offline:1112513178945986631>";
    public final static String BROWSER_ONLINE       = "<:browser_online:1112513381736398889>";
    public final static String BROWSER_IDLE         = "<:browser_idle:1112513425197772881>";
    public final static String BROWSER_DND          = "<:browser_dnd:1112513456109793331>";
    public final static String BROWSER_OFFLINE      = "<:browser_offline:1112513493262925935>";

    // status
    public final static String STATUS_STREAMING      = "<:statusStreaming:850113664873922560>";
    public final static String STATUS_ONLINE         = "<:statusOnline:850113664694353941>";
    public final static String STATUS_IDLE           = "<:statusAway:850113664873660427>";
    public final static String STATUS_DO_NOT_DISTURB = "<:statusDND:850113664873660426>";
    public final static String STATUS_INVISIBLE      = "<:statusInvisible:850113664643891262>";
    public final static String STATUS_OFFLINE        = "<:statusOffline:850113664556204124>";

    public static class LOGS {
        public static final LogEmoji BAN            = new LogEmoji("log_ban", 1118965051357921292L, 1118987411377102969L, 1118951198758797312L, 1118956271966638090L, 1118956677308366858L, 1118957191106404454L);
        public static final LogEmoji BOT            = new LogEmoji("log_bot", 1118965081389150309L, 1118987413507813386L, 1118951201204084857L, 1118956273942139043L, 1118956679195807865L, 1118957193178394737L);
        public static final LogEmoji CONNECT        = new LogEmoji("log_connect", 1118965084115435620L, 1118987415256838164L, 1118951202793734204L, 1118956275347238912L, 1118956680688980028L, 1118957194965176320L);
        public static final LogEmoji DEAFEN         = new LogEmoji("log_deafen", 1118965085503762485L, 1118987418641633410L, 1118951204282712174L, 1118956276852981800L, 1118956682089877555L, 1118957196575780965L);
        public static final LogEmoji DELETE         = new LogEmoji("log_delete", 1118965087844192387L, 1118987420453572640L, 1118951206656679946L, 1118956279075979364L, 1118956684056985600L, 1118957198299635743L);
        public static final LogEmoji DISCONNECT     = new LogEmoji("log_disconnect",     1118965129732698264L, 1118987455966744586L, 1118951284846886982L, 1118956308750671983L, 1118956723869339779L, 1118957365232926720L);
        public static final LogEmoji EDIT           = new LogEmoji("log_edit",           1118965131494305843L, 1118987458307162384L, 1118951287086665838L, 1118956310797504554L, 1118956726142648330L, 1118957367137144852L);
        public static final LogEmoji EMBED          = new LogEmoji("log_embed",          1118965136430997525L, 1118987463273234432L, 1118951292279193630L, 1118956315620946032L, 1118956729087033505L, 1118957370098331748L);
        public static final LogEmoji EMBED_REMOVE   = new LogEmoji("log_embed_remove",   1118965133780197506L, 1118987460693721230L, 1118951289011839107L, 1118956312521347082L, 1118956727690338326L, 1118957368567398531L);
        public static final LogEmoji GRAVEL         = new LogEmoji("log_gravel",         1118965138612039720L, 1118987464871268454L, 1118951293940142201L, 1118956317709705277L, 1118956732094357605L, 1118957372090634241L);
        public static final LogEmoji INVITE         = new LogEmoji("log_invite",         1118965210833764412L, 1118987502095704254L, 1118951349204287529L, 1118956358520275055L, 1118956777627721799L, 1118957411957473435L);
        public static final LogEmoji JOIN           = new LogEmoji("log_join",           1118965220686172181L, 1118987507510562916L, 1118951354103251024L, 1118956363008192522L, 1118956781905903616L, 1118957417003241514L);
        public static final LogEmoji JOIN_ALT       = new LogEmoji("log_join_alt",       1118965217523666975L, 1118987505149165578L, 1118951352689770610L, 1118956360848130158L, 1118956780265930753L, 1118957415401000970L);
        public static final LogEmoji KICK           = new LogEmoji("log_kick",           1118965219180425216L, 1118987509301530715L, 1118951356242350170L, 1118956365948403832L, 1118956784393125978L, 1118957419024887978L);
        public static final LogEmoji LEAVE          = new LogEmoji("log_leave",          1118965273307922616L, 1118987557523439647L, 1118951402824290384L, 1118956426325409933L, 1118956823744098325L, 1118957456702312508L);
        public static final LogEmoji LEAVE_ALT      = new LogEmoji("log_leave_alt",      1118965223240519710L, 1118987511222509599L, 1118951357651619880L, 1118956367508689018L, 1118956785882108015L, 1118957420396417134L);
        public static final LogEmoji MESSAGE_MUTE   = new LogEmoji("log_message_mute",   1118965276650766356L, 1118987559616381070L, 1118951405672202260L, 1118956428569362544L, 1118956825849643070L, 1118957458866585714L);
        public static final LogEmoji MESSAGE_UNMUTE = new LogEmoji("log_message_unmute", 1118965278219440178L, 1118987561608687706L, 1118951407089877042L, 1118956430188363867L, 1118956827682553907L, 1118957460481388616L);
        public static final LogEmoji MODERATION     = new LogEmoji("log_moderation",     1118965280530497626L, 1118987564397887518L, 1118951410327883877L, 1118956431601827880L, 1118956829247021148L, 1118957461836157091L);
        public static final LogEmoji MOVE           = new LogEmoji("log_move",           1118965282157895711L, 1118987565832355963L, 1118951411879776347L, 1118956433552191538L, 1118956835593015360L, 1118957464457588836L);
        public static final LogEmoji MUTE           = new LogEmoji("log_mute",           1118965336407023767L, 1118987610975633438L, 1118951467932471326L, 1118956517547323412L, 1118956882472738959L, 1118957535911743498L);
        public static final LogEmoji PROFILE_EDIT   = new LogEmoji("log_profile_edit",   1118965345517060186L, 1118987613890695258L, 1118951470000259253L, 1118956519577354381L, 1118956885660418128L, 1118957537916624976L);
        public static final LogEmoji PURGE          = new LogEmoji("log_purge",          1118965348318851082L, 1118987616197562489L, 1118951471468249260L, 1118956520877588630L, 1118956887543648286L, 1118957539330109480L);
        public static final LogEmoji REACT          = new LogEmoji("log_react",          1118965350445371533L, 1118987618961592492L, 1118951473070481418L, 1118956522408521851L, 1118956889951174677L, 1118957540718428270L);
        public static final LogEmoji ROLE           = new LogEmoji("log_role",           1118965353075200000L, 1118987621192978642L, 1118951474949542049L, 1118956524631511131L, 1118956891377238116L, 1118957542480027698L);
        public static final LogEmoji SERVER_EDIT    = new LogEmoji("log_server_edit",    1118965383064465509L, 1118987654126645318L, 1118951522861064274L, 1118956557430947963L, 1118956931709673493L, 1118957573530472529L);
        public static final LogEmoji THREAD_CREATE  = new LogEmoji("log_thread_create",  1118965386935795772L, 1118987656072810497L, 1118951525415399545L, 1118956560035614841L, 1118956934607945841L, 1118957575979925625L);
        public static final LogEmoji THREAD_DELETE  = new LogEmoji("log_thread_delete",  1118965390576451604L, 1118987658190929990L, 1118951527000850542L, 1118956561474273280L, 1118956936298233926L, 1118957576932048957L);
        public static final LogEmoji TIMEOUT        = new LogEmoji("log_timeout",        1118965393332125707L, 1118987661131124828L, 1118951528577896519L, 1118956563101667391L, 1118956938366025748L, 1118957578827874455L);
        public static final LogEmoji UNBAN          = new LogEmoji("log_unban",          1118965397882945556L, 1118987665128304770L, 1118951530767331458L, 1118956565295272047L, 1118956939829846026L, 1118957580664967209L);
        public static final LogEmoji UNDEAFEN       = new LogEmoji("log_undeafen",       1118965429818380308L, 1118987754852847626L, 1118951569648529540L, 1118956596886765648L, 1118956971505238037L, 1118957630757556355L);
        public static final LogEmoji UNGRAVEL       = new LogEmoji("log_ungravel",       1118965427238875299L, 1118987751266713762L, 1118951567232602183L, 1118956594340839474L, 1118956970087563306L, 1118957629125972089L);
        public static final LogEmoji UNMUTE         = new LogEmoji("log_unmute",         1118965424990715935L, 1118987748884353194L, 1118951565504557177L, 1118956592767971428L, 1118956968623743137L, 1118957627385327649L);
        public static final LogEmoji UNREACT        = new LogEmoji("log_unreact",        1118965421488484363L, 1118987746816569454L, 1118951563017326612L, 1118956589978767370L, 1118956966006497350L, 1118957625191710794L);
    }

    @AllArgsConstructor
    public static class LogEmoji {
        private final String name;
        private final long redId, yellowId, greenId, blueId, blurpleId, neutralId;

        public String redEmoji() {
            return emoji(redId);
        }

        public String yellowEmoji() {
            return emoji(yellowId);
        }

        public String greenEmoji() {
            return emoji(greenId);
        }

        public String blueEmoji() {
            return emoji(blueId);
        }

        public String blurpleEmoji() {
            return emoji(blurpleId);
        }

        public String neutralEmoji() {
            return emoji(neutralId);
        }

        public String redIcon(boolean large) {
            return icon(redId, large);
        }

        public String yellowIcon(boolean large) {
            return icon(yellowId, large);
        }

        public String greenIcon(boolean large) {
            return icon(greenId, large);
        }

        public String blueIcon(boolean large) {
            return icon(blueId, large);
        }

        public String blurpleIcon(boolean large) {
            return icon(blurpleId, large);
        }

        public String neutralIcon(boolean large) {
            return icon(neutralId, large);
        }

        private String emoji(long emojiId) {
            return String.format("<:%s:%d>", name, emojiId);
        }

        private String icon(long emojiId, boolean large) {
            return String.format("https://cdn.discordapp.com/emojis/%d.webp%s", emojiId, large ? "" : "?size=24&quality=lossless");
        }
    }
}
