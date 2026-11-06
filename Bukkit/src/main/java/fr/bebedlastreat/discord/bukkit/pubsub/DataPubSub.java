package fr.bebedlastreat.discord.bukkit.pubsub;

import fr.bebedlastreat.discord.bukkit.DiscordSyncBukkit;
import fr.bebedlastreat.discord.common.DiscordCommon;
import fr.bebedlastreat.discord.common.redis.PlayerDataCodec;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import redis.clients.jedis.JedisPubSub;

import java.util.logging.Level;

public class DataPubSub extends JedisPubSub {

    private final DiscordCommon common;

    public DataPubSub(DiscordCommon common) {
        this.common = common;
    }

    @Override
    public void onMessage(String channel, String message) {
        if (DiscordCommon.DATA_CHANNEL.equals(channel)) {
            PlayerDataCodec.PlayerData data;
            try {
                data = PlayerDataCodec.decode(message);
            } catch (IllegalArgumentException ex) {
                DiscordCommon.getLogger().log(Level.WARNING, "Received invalid player data from redis: " + ex.getMessage());
                return;
            }
            Bukkit.getScheduler().runTask(DiscordSyncBukkit.getInstance(), () -> {
                Player player = Bukkit.getPlayer(data.getUuid());
                if (player == null) {
                    return;
                }
                player.setMetadata("discord_linked", new FixedMetadataValue(DiscordSyncBukkit.getInstance(), data.isLinked()));
                player.setMetadata("discord_name", new FixedMetadataValue(DiscordSyncBukkit.getInstance(), data.getDiscordName()));
                player.setMetadata("discord_boosting", new FixedMetadataValue(DiscordSyncBukkit.getInstance(), data.isBoosting()));
            });

        }
    }
}
