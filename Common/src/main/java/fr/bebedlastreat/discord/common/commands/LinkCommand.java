package fr.bebedlastreat.discord.common.commands;

import fr.bebedlastreat.discord.common.DiscordCommon;
import fr.bebedlastreat.discord.common.interfaces.ICommonCommand;
import fr.bebedlastreat.discord.common.interfaces.ICommonCommandSender;
import fr.bebedlastreat.discord.common.interfaces.ICommonPlayer;
import fr.bebedlastreat.discord.common.objects.WaitingLink;
import fr.bebedlastreat.discord.redisbungee.RedisBungeeManager;

import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;

public class LinkCommand implements ICommonCommand {

    private final DiscordCommon common;

    public LinkCommand(DiscordCommon common) {
        this.common = common;
    }

    private void send(ICommonPlayer<?> player, String key, String fallback) {
        String message = common.getMessages().getOrDefault(key, fallback);
        common.getRunner().run(() -> player.sendMessage(message));
    }

    @Override
    public void execute(ICommonPlayer<?> player, String[] args) {
        if (args.length < 1) {
            player.sendMessage(common.getMessages().get("link-usage"));
            return;
        }

        common.getRunner().runAsync(() -> {
            if (common.getDatabaseFetch().exist(player.getUniqueId())) {
                send(player, "you-already-link", "");
                return;
            }
            String code = args[0];
            long now = System.currentTimeMillis();

            // Several Discord users may have requested a link for this player: look for the one matching the code,
            // so that a third party spamming /link cannot block the legitimate request.
            boolean pending = false;
            String matchKey = null;
            WaitingLink match = null;
            Iterator<Map.Entry<String, WaitingLink>> iterator = common.getWaitingLinks().entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, WaitingLink> entry = iterator.next();
                WaitingLink waitingLink = entry.getValue();
                if (waitingLink.getExpiry() < now) {
                    iterator.remove();
                    if (common.isRedisBungee()) {
                        RedisBungeeManager.removeWaitingLink(entry.getKey());
                    }
                    continue;
                }
                if (!waitingLink.getUuid().equals(player.getUniqueId())) continue;
                pending = true;
                if (code.equalsIgnoreCase(waitingLink.getCode())) {
                    matchKey = entry.getKey();
                    match = waitingLink;
                    break;
                }
            }

            if (match == null) {
                send(player, pending ? "invalid-code" : "no-link", "");
                return;
            }
            // consume the code atomically: a code can only be used once
            if (!common.removeWaitingLink(matchKey, match)) {
                send(player, "no-link", "");
                return;
            }
            if (common.getDatabaseFetch().exist(match.getDiscordId())) {
                send(player, "discord-already-linked", "<red>This Discord account is already linked to a Minecraft account.</red>");
                return;
            }
            if (!common.getDatabaseFetch().insert(match.getUuid(), player.getName(), match.getDiscordId())) {
                send(player, "link-error", "<red>An error occurred while linking your account, please try again later.</red>");
                return;
            }

            common.incrementLinkCount();
            common.rename(match.getDiscordId(), player.getName());
            send(player, "link-success", "");
            DiscordCommon.getLogger().log(Level.INFO, "Player " + player.getName() + " linked with " + match.getDiscordId());

            boolean firstLink = common.getDatabaseFetch().firstLink(player.getUniqueId());
            if (firstLink) {
                DiscordCommon.getLogger().log(Level.INFO, "Player " + player.getName() + " linked for the first time");
                common.getDatabaseFetch().insertFirstLink(player.getUniqueId());
                common.incrementAllTimeLinkCount();
            }
            if ((firstLink || !common.isOneTimeReward()) && !common.getRewardCommand().isEmpty()) {
                common.getRunner().runLater(() -> {
                    for (String command : common.getRewardCommand()) {
                        common.getConsoleExecutor().execute(command.replace("{player}", player.getName()).replace("{uuid}", player.getUniqueId().toString()), player);
                    }
                }, 0);
            }
        });
    }

    @Override
    public void execute(ICommonCommandSender<?> sender, String[] args) {

    }
}
