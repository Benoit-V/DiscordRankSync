package fr.bebedlastreat.discord.bukkit.pubsub;

import fr.bebedlastreat.discord.bukkit.DiscordSyncBukkit;
import fr.bebedlastreat.discord.common.DiscordCommon;
import org.bukkit.Bukkit;
import redis.clients.jedis.JedisPubSub;

public class CommandPubSub extends JedisPubSub {

    private final DiscordCommon common;

    public CommandPubSub(DiscordCommon common) {
        this.common = common;
    }

    @Override
    public void onMessage(String channel, String message) {
        if (DiscordCommon.COMMAND_CHANNEL.equals(channel)) {
            // only run commands signed by a proxy sharing redis.secret: anyone able to publish on redis
            // must not be able to run console commands
            String command = common.getRedisHandler().verifyCommand(message);
            if (command == null) return;
            Bukkit.getScheduler().runTask(DiscordSyncBukkit.getInstance(), () -> {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            });
        }
    }
}
