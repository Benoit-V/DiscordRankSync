package fr.bebedlastreat.discord.common.listeners;

import fr.bebedlastreat.discord.common.DiscordCommon;
import fr.bebedlastreat.discord.common.objects.WaitingLink;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import java.awt.*;
import java.security.SecureRandom;
import java.util.UUID;
import java.util.logging.Level;

public class SlashCommandListener extends ListenerAdapter {

    private static final long LINK_EXPIRY = 1000L * 60 * 5;

    private final DiscordCommon common;
    private final SecureRandom random = new SecureRandom();

    public SlashCommandListener(DiscordCommon common) {
        this.common = common;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent e) {
        if (common.isStandalone()) return;
        String name = e.getName().toLowerCase();
        if (name.equals("ping")) {
            long time = System.currentTimeMillis();
            e.reply("Pong!").setEphemeral(false)
                    .flatMap(v -> e.getHook().editOriginalFormat("Pong: %d ms", System.currentTimeMillis() - time))
                    .queue();
            return;
        }
        if (!name.equals("minecraft") && !name.equals("discord") && !name.equals("link") && !name.equals("unlink")) {
            return;
        }

        // database and Minecraft lookups are blocking: acknowledge now (Discord only waits 3 seconds) and answer asynchronously
        boolean ephemeral = name.equals("link") || name.equals("unlink");
        e.deferReply(ephemeral).queue();
        InteractionHook hook = e.getHook();
        common.getRunner().runAsync(() -> {
            MessageEmbed embed;
            try {
                embed = handle(e, name);
            } catch (Exception ex) {
                DiscordCommon.getLogger().log(Level.SEVERE, "Error while handling /" + name + ": " + ex.getMessage());
                embed = error("An internal error occurred");
            }
            hook.sendMessageEmbeds(embed).queue();
        });
    }

    private MessageEmbed handle(SlashCommandInteractionEvent e, String name) {
        switch (name) {
            case "minecraft": {
                OptionMapping option = e.getOption(common.getMessages().get("name"));
                User target = option == null ? null : option.getAsUser();
                String idStr = target == null ? "" : common.getDatabaseFetch().uuid(target.getId());
                if (idStr.isEmpty()) {
                    return new EmbedBuilder().setDescription(common.getMessages().get("no-minecraft")).setColor(Color.RED).build();
                }
                UUID uuid = UUID.fromString(idStr);
                String playerName = common.getDatabaseFetch().name(uuid);
                return new EmbedBuilder().setDescription(common.getMessages().get("results") + "`" + uuid + " (" + playerName + ")" + "`").setColor(Color.GREEN).build();
            }
            case "discord": {
                OptionMapping option = e.getOption(common.getMessages().get("name"));
                String discordId = option == null ? "" : common.getDatabaseFetch().discord(option.getAsString());
                if (discordId.length() < 10) {
                    return new EmbedBuilder().setDescription(common.getMessages().get("no-discord")).setColor(Color.RED).build();
                }
                return new EmbedBuilder().setDescription(common.getMessages().get("results") + "`" + discordId + "` <@" + discordId + ">").setColor(Color.GREEN).build();
            }
            case "link": {
                OptionMapping option = e.getOption(common.getMessages().get("name"));
                User user = e.getUser();
                if (common.getDatabaseFetch().exist(user.getId())) {
                    return error(common.getMessages().get("already-link-minecraft"));
                }
                UUID uuid = option == null ? null : common.getOnlineCheck().getUuid(option.getAsString());
                if (uuid == null || !common.getOnlineCheck().isOnline(uuid)) {
                    return error(common.getMessages().get("not-online"));
                }
                if (common.getDatabaseFetch().exist(uuid)) {
                    return error(common.getMessages().get("already-link-discord"));
                }
                String code = getRandomNumber();
                common.addWaitingLink(user.getId(), new WaitingLink(user.getId(), System.currentTimeMillis() + LINK_EXPIRY, uuid, code));
                return new EmbedBuilder().setTitle(common.getMessages().get("validation")).setDescription(common.getMessages().get("validation-desc").replace("{id}", code)).setColor(Color.GREEN).build();
            }
            case "unlink": {
                User user = e.getUser();
                if (!common.getDatabaseFetch().exist(user.getId())) {
                    return error(common.getMessages().get("no-link-minecraft"));
                }
                common.unlinkByDiscordId(user.getId(), true);
                return new EmbedBuilder().setTitle(common.getMessages().get("success")).setDescription(common.getMessages().get("success-desc")).setColor(Color.GREEN).build();
            }
            default:
                throw new IllegalArgumentException("Unknown command " + name);
        }
    }

    private MessageEmbed error(String description) {
        return new EmbedBuilder().setTitle(common.getMessages().get("error")).setDescription(description).setColor(Color.RED).build();
    }

    private String getRandomNumber() {
        return String.format("%06d", random.nextInt(1_000_000));
    }
}
