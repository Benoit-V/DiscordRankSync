package fr.bebedlastreat.discord.common.listeners;

import fr.bebedlastreat.discord.common.DiscordCommon;
import fr.bebedlastreat.discord.common.objects.WaitingLink;
import fr.bebedlastreat.discord.redisbungee.RedisBungeeManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import java.awt.*;
import java.util.Random;
import java.util.UUID;

public class SlashCommandListener extends ListenerAdapter {

    private final DiscordCommon common;

    public SlashCommandListener(DiscordCommon common) {
        this.common = common;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent e) {

        if (common.isStandalone()) return;
        if (e.getName().equalsIgnoreCase("ping")) {
            long time = System.currentTimeMillis();
            e.reply("Pong!").setEphemeral(false)
                    .flatMap(v ->
                            e.getHook().editOriginalFormat("Pong: %d ms", System.currentTimeMillis() - time) // then edit original
                    ).queue(); // Queue both reply and edit
        }
        if (e.getName().equalsIgnoreCase("minecraft")) {
            OptionMapping option = e.getOption(common.getMessages().get("name"));
            String targeId = option.getAsMember().getId();
            String idStr = common.getDatabaseFetch().uuid(targeId);
            if (idStr.isEmpty()) {
                e.replyEmbeds(new EmbedBuilder().setDescription(common.getMessages().get("no-minecraft")).setColor(Color.RED).build()).queue();
            } else {
                UUID uuid = UUID.fromString(idStr);
                String name = common.getDatabaseFetch().name(uuid);
                e.replyEmbeds(new EmbedBuilder().setDescription(common.getMessages().get("results") + "`" + uuid + " (" + name + ")" + "`").setColor(Color.GREEN).build()).queue();
            }
        }
        if (e.getName().equalsIgnoreCase("discord")) {
            OptionMapping option = e.getOption(common.getMessages().get("name"));
            String name = option.getAsString();
            String discordId = common.getDatabaseFetch().discord(name);
            if (discordId == null || discordId.length() < 10) {
                e.replyEmbeds(new EmbedBuilder().setDescription(common.getMessages().get("no-discord")).setColor(Color.RED).build()).queue();
            } else {
                e.replyEmbeds(new EmbedBuilder().setDescription(common.getMessages().get("results") + "`" + discordId + "` <@" + discordId + ">").setColor(Color.GREEN).build()).queue();
            }
        }
        if (e.getName().equalsIgnoreCase("link")) {
            OptionMapping option = e.getOption(common.getMessages().get("name"));
            String name = option.getAsString();
            User user = e.getUser();
            if (common.getDatabaseFetch().exist(user.getId())) {
                e.replyEmbeds(new EmbedBuilder().setTitle(common.getMessages().get("error")).setDescription(common.getMessages().get("already-link-minecraft")).setColor(Color.RED).build()).setEphemeral(true).queue();
                return;
            }
            UUID uuid = common.getOnlineCheck().getUuid(name);
            if (uuid == null || !common.getOnlineCheck().isOnline(uuid)) {
                e.replyEmbeds(new EmbedBuilder().setTitle(common.getMessages().get("error")).setDescription(common.getMessages().get("not-online")).setColor(Color.RED).build()).setEphemeral(true).queue();
            } else {
                if (common.getDatabaseFetch().exist(uuid)) {
                    e.replyEmbeds(new EmbedBuilder().setTitle(common.getMessages().get("error")).setDescription(common.getMessages().get("already-link-discord")).setColor(Color.RED).build()).setEphemeral(true).queue();
                    return;
                }
                String randomId = getRandomNumber();
                e.replyEmbeds(new EmbedBuilder().setTitle(common.getMessages().get("validation")).setDescription(common.getMessages().get("validation-desc").replace("{id}", randomId)).setColor(Color.GREEN).build()).setEphemeral(true).queue();
                WaitingLink waitingLink = new WaitingLink(user.getId(), System.currentTimeMillis() + 1000 * 60 * 5, uuid, randomId);
                common.getWaitingLinks().put(user.getId(), waitingLink);
                if (common.isRedisBungee()) {
                    RedisBungeeManager.addWaitingLink(user.getId(), waitingLink);
                }
            }
        }
        if (e.getName().equalsIgnoreCase("unlink")) {
            User user = e.getUser();
            if (!common.getDatabaseFetch().exist(user.getId())) {
                e.replyEmbeds(new EmbedBuilder().setTitle(common.getMessages().get("error")).setDescription(common.getMessages().get("no-link-minecraft")).setColor(Color.RED).build()).setEphemeral(true).queue();
                return;
            }
            common.unlinkByDiscordId(user.getId(), true);
            e.replyEmbeds(new EmbedBuilder().setTitle(common.getMessages().get("success")).setDescription(common.getMessages().get("success-desc")).setColor(Color.GREEN).build()).setEphemeral(true).queue();
        }
    }

    private final Random random = new Random();

    private String getRandomNumber() {
        String s = "";
        for (int i = 0; i < 6; i++) {
            s += String.valueOf(random.nextInt(10));
        }
        return s;
    }
}