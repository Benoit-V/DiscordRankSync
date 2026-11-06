package fr.bebedlastreat.discord.bungeecord.implementations;

import fr.bebedlastreat.discord.bungeecord.utils.DiscordBungeePluginMessage;
import fr.bebedlastreat.discord.common.DiscordCommon;
import fr.bebedlastreat.discord.common.interfaces.ICommonPlayer;
import fr.bebedlastreat.discord.common.interfaces.IConsoleExecutor;
import net.md_5.bungee.api.ProxyServer;

import java.util.logging.Level;

public class BungeeConsoleExecutor implements IConsoleExecutor {
    @Override
    public void execute(String command, ICommonPlayer<?> target) {
        if (command.startsWith("[FORWARD] ") && target != null) {
            command = command.substring(10);
            if (DiscordCommon.getInstance().isRedisEnabled()) {
                // signed with redis.secret, backends reject unsigned commands
                DiscordCommon.getInstance().getRedisHandler().sendCommand(command);
            } else {
                DiscordCommon.getLogger().log(Level.WARNING, "Redis is not enabled, can't forward command: " + command);
            }
        } else {
            ProxyServer.getInstance().getPluginManager().dispatchCommand(ProxyServer.getInstance().getConsole(), command);
        }
    }
}
