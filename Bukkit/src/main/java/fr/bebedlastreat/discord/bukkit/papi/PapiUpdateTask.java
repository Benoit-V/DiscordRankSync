package fr.bebedlastreat.discord.bukkit.papi;

import fr.bebedlastreat.discord.bukkit.implementations.SpigotPlayer;
import fr.bebedlastreat.discord.common.DiscordCommon;
import fr.bebedlastreat.discord.bukkit.DiscordSyncBukkit;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

public class PapiUpdateTask extends BukkitRunnable {
    private final DiscordCommon common;

    public PapiUpdateTask(DiscordCommon common) {
        this.common = common;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            common.getData(new SpigotPlayer(player), (linked, discordName, boosting) -> {
                if (player.isOnline()) {
                    player.setMetadata("discord_linked", new FixedMetadataValue(DiscordSyncBukkit.getInstance(), linked));
                    player.setMetadata("discord_name", new FixedMetadataValue(DiscordSyncBukkit.getInstance(), discordName));
                    player.setMetadata("discord_boosting", new FixedMetadataValue(DiscordSyncBukkit.getInstance(), boosting));
                }
            });
        }
        if (!common.isStandalone()) {
            DiscordPapi.setMemberCount(common.retrieveMemberCount(false));
        }
    }
}
