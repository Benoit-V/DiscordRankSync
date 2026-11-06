package fr.bebedlastreat.discord.bukkit.papi;

import fr.bebedlastreat.discord.bukkit.DiscordSyncBukkit;
import fr.bebedlastreat.discord.common.DiscordCommon;
import fr.bebedlastreat.discord.common.utils.NonOverlappingTask;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;

@UtilityClass
public class DiscordPapi {

    @Getter
    @Setter
    private int memberCount;

    public void init() {
        new DiscordPlaceHolderExpansion(DiscordSyncBukkit.getInstance(), DiscordCommon.getInstance()).register();
        if (!DiscordCommon.getInstance().isStandalone()) {
            Bukkit.getPluginManager().registerEvents(new PapiListener(DiscordCommon.getInstance()), DiscordSyncBukkit.getInstance());
            // first run immediately (async: it does blocking Discord calls), then every refresh-delay seconds
            Bukkit.getScheduler().runTaskTimerAsynchronously(DiscordSyncBukkit.getInstance(),
                    new NonOverlappingTask("PlaceholderAPI refresh", new PapiUpdateTask(DiscordCommon.getInstance())),
                    0L, DiscordCommon.getInstance().getRefreshDelay()*20L);
        }
    }
}
