package fr.bebedlastreat.discord.common.listeners;

import fr.bebedlastreat.discord.common.DiscordCommon;
import fr.bebedlastreat.discord.common.interfaces.ICommonListener;
import fr.bebedlastreat.discord.common.interfaces.ICommonPlayer;
import fr.bebedlastreat.discord.common.objects.DiscordRank;

import java.util.ArrayList;
import java.util.List;

public class JoinListener implements ICommonListener {

    private final DiscordCommon common;

    public JoinListener(DiscordCommon common) {
        this.common = common;
    }

    public void execute(ICommonPlayer<?> player) {
        common.getRunner().runAsync(() -> {
            String discord = common.getDatabaseFetch().discord(player.getUniqueId());
            if (!discord.isEmpty()) {
                common.rename(discord, player.getName());
                List<DiscordRank> toAdd = new ArrayList<>();
                List<DiscordRank> toRemove = new ArrayList<>();
                for (DiscordRank rank : common.getRanks()) {
                    if (player.hasPermission(rank.getPermission())) {
                        toAdd.add(rank);
                    } else {
                        toRemove.add(rank);
                    }
                }
                common.syncRoles(discord, toAdd, toRemove);
                common.getDatabaseFetch().update(player.getUniqueId(), player.getName());
            } else {
                String joinMessage = common.getMessages().getOrDefault("join-message", "");
                if (!joinMessage.isEmpty()) {
                    common.getRunner().runLater(() -> {
                        player.sendMessage(joinMessage);
                    }, common.getJoinMessageDelay());
                }
            }
        });
    }
}
