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

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import com.jagrosh.jdautilities.commons.utils.FinderUtil;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.commands.CommandExceptionListener.CommandErrorException;
import com.jagrosh.vortex.commands.CommandTools;
import com.jagrosh.vortex.utils.DiscordPallete;
import com.jagrosh.vortex.utils.FormatUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.TimeFormat;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.apache.commons.collections4.map.ListOrderedMap;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * @author John Grosh (jagrosh)
 */
public class RoleInfoCmd extends SlashCommand {
    public static final ListOrderedMap<Permission, String> PERMISSION_NAME_MAP;
    public static final Comparator<Permission> PERMISSION_COMPARATOR;

    private final Vortex vortex;

    static {
         PERMISSION_NAME_MAP = new ListOrderedMap<>() {{
            put(Permission.ADMINISTRATOR, "Administrator");
            put(Permission.MANAGE_SERVER, "Manage Server");
            put(Permission.MANAGE_CHANNEL, "Manage Channels");
            put(Permission.MANAGE_ROLES, "Manage Roles");
            put(Permission.MESSAGE_MANAGE, "Manage Messages");
            put(Permission.MANAGE_GUILD_EXPRESSIONS, "Manage Expressions");
            put(Permission.MANAGE_WEBHOOKS, "Manage Webhooks");
            put(Permission.MANAGE_EVENTS, "Manage Events");
            put(Permission.MANAGE_THREADS, "Manage Threads");
            put(Permission.MESSAGE_MENTION_EVERYONE, "Ping @everyone");
            put(Permission.KICK_MEMBERS, "Kick");
            put(Permission.BAN_MEMBERS, "Ban");
            put(Permission.MODERATE_MEMBERS, "Timeout");
            put(Permission.VOICE_MUTE_OTHERS, "Voice Mute");
            put(Permission.VOICE_DEAF_OTHERS, "Deafen");
            put(Permission.VOICE_MOVE_OTHERS, "Voice Move");
            put(Permission.CREATE_PUBLIC_THREADS, "Create Public Threads");
            put(Permission.CREATE_PRIVATE_THREADS, "Create Private Threads");
            put(Permission.NICKNAME_MANAGE, "Manage Nicks");
            put(Permission.VIEW_AUDIT_LOGS, "View Auditlog");
            put(Permission.VIEW_GUILD_INSIGHTS, "View Server Insights");
            put(Permission.VIEW_CREATOR_MONETIZATION_ANALYTICS, "View Creator Analytics");
            put(Permission.CREATE_INSTANT_INVITE, "Create Invites");
            put(Permission.NICKNAME_CHANGE, "Change Nick");
            put(Permission.MESSAGE_ADD_REACTION, "React");
            put(Permission.MESSAGE_EMBED_LINKS, "Embed");
            put(Permission.MESSAGE_ATTACH_FILES, "Upload Files");
            put(Permission.MESSAGE_ATTACH_VOICE_MESSAGE, "Upload Voice Messages");
            put(Permission.MESSAGE_TTS, "Use TTS");
            put(Permission.MESSAGE_EXT_EMOJI, "External Emojis");
            put(Permission.MESSAGE_EXT_STICKER, "External Stickers");
            put(Permission.VOICE_STREAM, "Stream");
            put(Permission.VOICE_USE_SOUNDBOARD, "Use Soundboard");
            put(Permission.VOICE_USE_EXTERNAL_SOUNDS, "Use External Sounds");
            put(Permission.PRIORITY_SPEAKER, "Priority Speaker");
            put(Permission.REQUEST_TO_SPEAK, "Request To Speak");
            put(Permission.VOICE_USE_VAD, "Use Voice Activity");
            put(Permission.VOICE_SPEAK, "Speak In VC");
            put(Permission.VOICE_CONNECT, "Connect To VC");
            put(Permission.VOICE_START_ACTIVITIES, "Use Activities");
            put(Permission.USE_APPLICATION_COMMANDS, "Use Slash Commands");
            put(Permission.MESSAGE_HISTORY, "See History");
            put(Permission.MESSAGE_SEND, "Send Messages");
            put(Permission.MESSAGE_SEND_IN_THREADS, "Send Messages In Threads");
            put(Permission.VIEW_CHANNEL, "View Channels");
        }};

        PERMISSION_COMPARATOR = Comparator.comparingInt(permission -> {
            if (permission == Permission.UNKNOWN) {
                return Integer.MAX_VALUE;
            } else {
                int index = PERMISSION_NAME_MAP.indexOf(permission);
                if (index == -1) {
                    index = PERMISSION_NAME_MAP.size() + permission.ordinal();
                }
                return index;
            }
        });
    }

    public RoleInfoCmd(Vortex vortex) {
        this.name = "roleinfo";
        this.aliases = new String[]{"rinfo", "rankinfo"};
        this.help = "shows info about a role";
        this.arguments = "<role>";
        this.guildOnly = true;
        this.vortex = vortex;
        this.options = Collections.singletonList(new OptionData(OptionType.ROLE, "role", "The role", true));
    }

    @Override
    protected void execute(SlashCommandEvent event) {
        if (!CommandTools.hasGeneralCommandPerms(vortex, event, Permission.MESSAGE_MANAGE)) {
            event.reply(CommandTools.COMMAND_NO_PERMS).setEphemeral(true).queue();
            return;
        }

        event.reply(getRoleInfoEmbed(event.getOption("role").getAsRole())).queue();
    }

    @Override
    protected void execute(CommandEvent event) {
        if (!CommandTools.hasGeneralCommandPerms(vortex, event, Permission.MESSAGE_MANAGE)) {
            return;
        }

        if (event.getArgs().isEmpty()) {
            throw new CommandErrorException("Please provide the name of a role!");
        } else {
            List<Role> found = FinderUtil.findRoles(event.getArgs(), event.getGuild());
            if (found.isEmpty()) {
                event.replyError("I couldn't find the role you were looking for!");
            } else if (found.size() > 1) {
                event.replyWarning(FormatUtil.filterEveryone(FormatUtil.listOfRoles(found, event.getArgs())));
            } else {
                event.reply(getRoleInfoEmbed(found.get(0)));
            }
        }

    }

    public MessageCreateData getRoleInfoEmbed(Role role) {
        Guild g = role.getGuild();
        int totalRoles = (int) g.getRoleCache().size();
        int position = totalRoles - role.getPosition() - 1;
        EmbedBuilder builder = new EmbedBuilder().setColor(role.getColor() == null ? DiscordPallete.DEFAULT_ROLE_WHITE : role.getColor())
                .setDescription("## Showing Info For " + role.getAsMention())
                .addField("ID", role.getId(), true)
                .addField("Color", FormatUtil.formatRoleColor(role), true)
                .addField("Created", TimeFormat.DATE_SHORT.format(role.getTimeCreated()), true)
                .addField("Hoisted", role.isHoisted() ? "Yes" : "No", true)
                .addField("Position", (position + "/" + totalRoles), true);
        if (role.getIcon() != null) {
            builder.addField("Images", "[Icon](" + role.getIcon().getIconUrl() + ")", true);
        }

        String formattedPermissions = FormatUtil.formatRolePermissions(role);
        builder.addField("Permissions", formattedPermissions, formattedPermissions.length() <= 32); // Arbitrary # for formatting

        if (role.isPublicRole()) {
            builder.appendDescription("\nThis is the special @everyone role, which everyone technically has");
        } else if (Objects.equals(role, g.getBoostRole())) {
            builder.appendDescription("\nThis is the server booster role");
        } else if (role.isManaged()) {
            builder.appendDescription("\nThis role is managed by an integration");
        }

        return MessageCreateData.fromEmbeds(builder.build());
    }
}
