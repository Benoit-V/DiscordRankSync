package fr.bebedlastreat.discord.bungeecord.utils;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import fr.bebedlastreat.discord.bungeecord.implementations.BungeePlayer;
import fr.bebedlastreat.discord.common.DiscordCommon;
import fr.bebedlastreat.discord.common.redis.PlayerDataCodec;
import lombok.experimental.UtilityClass;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;

import java.util.logging.Level;

@UtilityClass
public class DiscordBungeePluginMessage {

    public void sendData(ProxiedPlayer player, boolean linked, String discordName, boolean boost) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeBoolean(linked);
        out.writeUTF(discordName);
        out.writeBoolean(boost);

        Server server = player.getServer();
        if (server != null) {
            ServerInfo info = server.getInfo();
            if (info != null) {
                player.getServer().getInfo().sendData(DiscordCommon.DATA_CHANNEL, out.toByteArray());
            }
        }
    }

    public void sendData(ProxiedPlayer player) {
        DiscordCommon.getInstance().getData(new BungeePlayer(player), (linked, discordName, boosting) -> {
            DiscordBungeePluginMessage.sendData(player, linked, discordName, boosting);
            if (DiscordCommon.getInstance().isRedisEnabled()) {
                String message = PlayerDataCodec.encode(player.getUniqueId(), linked, discordName, boosting);
                DiscordCommon.getInstance().getRedisHandler().send(DiscordCommon.DATA_CHANNEL, message);
            }
        });
    }

    /*public void sendCommand(ProxiedPlayer player, String command) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(command);
        Server server = player.getServer();
        if (server != null) {
            ServerInfo info = server.getInfo();
            if (info != null) {
                player.getServer().getInfo().sendData(DiscordCommon.COMMAND_CHANNEL, out.toByteArray());
            }
        } else {
            DiscordCommon.getLogger().log(Level.WARNING, "Can't dispatch command for " + player.getName() + " -> /" + command);
        }
    }*/
}
