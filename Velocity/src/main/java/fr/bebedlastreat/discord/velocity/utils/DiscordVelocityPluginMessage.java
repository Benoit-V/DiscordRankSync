package fr.bebedlastreat.discord.velocity.utils;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import fr.bebedlastreat.discord.common.DiscordCommon;
import fr.bebedlastreat.discord.common.redis.PlayerDataCodec;
import fr.bebedlastreat.discord.velocity.implementations.VelocityPlayer;
import lombok.experimental.UtilityClass;

import java.util.Optional;
import java.util.logging.Level;

@UtilityClass
public class DiscordVelocityPluginMessage {

    public void sendData(Player player, boolean linked, String discordName, boolean boost) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeBoolean(linked);
        out.writeUTF(discordName);
        out.writeBoolean(boost);

        Optional<ServerConnection> currentServer = player.getCurrentServer();
        currentServer.ifPresent(serverConnection -> {
            try {
                serverConnection.sendPluginMessage(MinecraftChannelIdentifier.from(DiscordCommon.DATA_CHANNEL), out.toByteArray());
            } catch (Exception ignore) {}
        });
    }

    public void sendData(Player player) {
        DiscordCommon.getInstance().getData(new VelocityPlayer(player), (linked, discordName, boosting) -> {
            DiscordVelocityPluginMessage.sendData(player, linked, discordName, boosting);
            if (DiscordCommon.getInstance().isRedisEnabled()) {
                String message = PlayerDataCodec.encode(player.getUniqueId(), linked, discordName, boosting);
                DiscordCommon.getInstance().getRedisHandler().send(DiscordCommon.DATA_CHANNEL, message);
            }
        });
    }

    /*public void sendCommand(Player player, String command) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(command);

        Optional<ServerConnection> currentServer = player.getCurrentServer();
        currentServer.ifPresent(serverConnection -> {
            try {
                serverConnection.sendPluginMessage(MinecraftChannelIdentifier.from(DiscordCommon.COMMAND_CHANNEL), out.toByteArray());
            } catch (Exception ignore) {
                DiscordCommon.getLogger().log(Level.WARNING, "Can't dispatch command for " + player.getUsername() + " -> /" + command);
            }
        });
    }*/
}
