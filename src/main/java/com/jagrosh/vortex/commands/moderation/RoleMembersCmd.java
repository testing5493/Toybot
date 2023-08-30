package com.jagrosh.vortex.commands.moderation;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import com.jagrosh.jdautilities.commons.utils.FinderUtil;
import com.jagrosh.jdautilities.menu.EmbedPaginator;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.commands.CommandExceptionListener;
import com.jagrosh.vortex.commands.HybridEvent;
import com.jagrosh.vortex.utils.DiscordPallete;
import com.jagrosh.vortex.utils.FormatUtil;
import com.jagrosh.vortex.utils.OtherUtil;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RoleMembersCmd extends ModCommand {
    public RoleMembersCmd(Vortex vortex) {
        super(vortex, Permission.MANAGE_ROLES);
        this.name = "userswith";
        this.aliases = new String[]{"usersin", "membersin", "memberswith", "roleusers", "rolemembers", "members"};
        this.arguments = "<role>";
        this.help = "Shows users with a role";
        this.guildOnly = true;
        this.options = new LinkedList<>() {{
            add(new OptionData(OptionType.ROLE, "role", "The role to search", true));
        }};
    }

    @Override
    protected void execute(SlashCommandEvent event) {
        InteractionHook hook = event.deferReply().complete();
        paginate(event.optRole("role", event.getGuild().getPublicRole()), HybridEvent.of(event));
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getArgs().isEmpty()) {
            throw new CommandExceptionListener.CommandErrorException("Please provide the name of a role!");
        } else {
            List<Role> found = FinderUtil.findRoles(event.getArgs(), event.getGuild());
            if (found.isEmpty()) {
                event.replyError("I couldn't find the role you were looking for!");
            } else if (found.size() > 1) {
                event.replyWarning(FormatUtil.filterEveryone(FormatUtil.listOfRoles(found, event.getArgs())));
            } else {
                event.getChannel().sendTyping().queue();
                paginate(found.get(0), HybridEvent.of(event));
            }
        }
    }

    private void paginate(Role role, HybridEvent event) {
        role.getGuild().findMembersWithRoles(role).onError(t -> {
            event.replyError("Something went wrong. Please try again");
            log.error("Failed to resolve users with role " + role.getId() + " in guild " + role.getGuild().getId(), t);
        }).onSuccess(members -> {
            try {
                if (members.isEmpty()) {
                    event.reply("No members found with " + role.getAsMention());
                    return;
                }

                try {
                    members.sort(Comparator.comparing(Member::getEffectiveName));
                } catch (UnsupportedOperationException e) {
                    List<Member> temp = new ArrayList<>(members);
                    temp.sort(Comparator.comparing(Member::getEffectiveName));
                    members = temp;
                }

                final int MAX_PER_PAGE = 16;
                EmbedBuilder[] embeds = new EmbedBuilder[OtherUtil.pagintatorSize(members.size(), MAX_PER_PAGE)];

                Color color = role.getColor() != null ? role.getColor() : DiscordPallete.DEFAULT_ROLE_WHITE;
                for (int i = 0; i < embeds.length; i++) {
                    embeds[i] = new EmbedBuilder().setColor(color);
                    if (embeds.length > 1) {
                        embeds[i].setFooter("Page " + (i + 1) + " of " + embeds.length);
                    }
                }

                for (int i = 0; i < members.size(); i++) {
                    embeds[i / MAX_PER_PAGE].appendDescription(members.get(i).getAsMention()).appendDescription("\n");
                }

                EmbedPaginator.Builder paginatorBuilder = new EmbedPaginator.Builder();
                for (EmbedBuilder embed : embeds) {
                    // Remove trailing newlines in embeds before building
                    StringBuilder descriptionBuilder = embed.getDescriptionBuilder();
                    descriptionBuilder.deleteCharAt(descriptionBuilder.length() - 1);
                    paginatorBuilder.addItems(embed.build());
                }

                // Sends the message
                EmbedPaginator paginator = paginatorBuilder
                        .setText(String.format("%d user%s found with %s", members.size(), members.size() == 1 ? "" : "s", role.getAsMention()))
                        .wrapPageEnds(true)
                        .setEventWaiter(vortex.getEventWaiter())
                        .setTimeout(20, TimeUnit.MINUTES)
                        .setFinalAction(m -> m.clearReactions().queue())
                        .build();
                if (event.isSlashCommandEvent()) {
                    paginator.display(event.getSlashCommandEvent().getHook());
                } else {
                    paginator.display(event.getCommandEvent().getChannel());
                }
            } catch (Exception e) {
                event.replyError("Something went wrong. Please try again");
                log.error("Failed to generate paginator for users with role " + role.getId() + " in guild " + role.getGuild().getId(), e);
            }
        });
    }
}
