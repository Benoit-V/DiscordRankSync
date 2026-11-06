package fr.bebedlastreat.discord.bungeecord.listeners;

import fr.bebedlastreat.discord.bungeecord.DiscordSyncBungee;
import fr.bebedlastreat.discord.bungeecord.implementations.BungeePlayer;
import fr.bebedlastreat.discord.bungeecord.utils.DiscordBungeePluginMessage;
import fr.bebedlastreat.discord.common.DiscordCommon;
import fr.bebedlastreat.discord.common.objects.DiscordMember;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.concurrent.TimeUnit;

public class BungeeJoinListener implements Listener {

    private final DiscordCommon common;

    public BungeeJoinListener(DiscordCommon common) {
        this.common = common;
    }

    @EventHandler
    public void onJoin(PostLoginEvent e) {
        ProxiedPlayer player = e.getPlayer();
        common.getJoinListener().execute(new BungeePlayer(player));
        ProxyServer.getInstance().getScheduler().schedule(DiscordSyncBungee.getInstance(), () -> {
            ProxyServer.getInstance().getScheduler().runAsync(DiscordSyncBungee.getInstance(), () -> {
                DiscordBungeePluginMessage.sendData(player);
            });
        }, 100, TimeUnit.MILLISECONDS);
    }

    @EventHandler
    public void onServer(ServerConnectedEvent e) {
        ProxiedPlayer player = e.getPlayer();
        ProxyServer.getInstance().getScheduler().schedule(DiscordSyncBungee.getInstance(), () -> {
            ProxyServer.getInstance().getScheduler().runAsync(DiscordSyncBungee.getInstance(), () -> {
                DiscordBungeePluginMessage.sendData(player);
            });
        }, 100, TimeUnit.MILLISECONDS);
    }
}
