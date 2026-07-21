package fr.bebedlastreat.discord.bungeecord;

import fr.bebedlastreat.discord.bungeecord.commands.BungeeClaimBoostCommand;
import fr.bebedlastreat.discord.bungeecord.commands.BungeeLinkCommand;
import fr.bebedlastreat.discord.bungeecord.commands.BungeeStopbotCommand;
import fr.bebedlastreat.discord.bungeecord.commands.BungeeUnlinkCommand;
import fr.bebedlastreat.discord.bungeecord.implementations.BungeeConsoleExecutor;
import fr.bebedlastreat.discord.bungeecord.implementations.BungeeOnlineCheck;
import fr.bebedlastreat.discord.bungeecord.implementations.BungeeRunner;
import fr.bebedlastreat.discord.bungeecord.listeners.BungeeJoinListener;
import fr.bebedlastreat.discord.bungeecord.listeners.RedisBungeeListener;
import fr.bebedlastreat.discord.bungeecord.utils.DiscordBungeePluginMessage;
import fr.bebedlastreat.discord.common.DiscordCommon;
import fr.bebedlastreat.discord.common.charts.*;
import fr.bebedlastreat.discord.common.enums.DatabaseType;
import fr.bebedlastreat.discord.common.enums.ServerType;
import fr.bebedlastreat.discord.common.logger.DefaultLogger;
import fr.bebedlastreat.discord.common.objects.DiscordActivity;
import fr.bebedlastreat.discord.common.objects.DiscordRank;
import fr.bebedlastreat.discord.common.sql.SqlHandler;
import fr.bebedlastreat.discord.common.sqlite.SQLiteHandler;
import fr.bebedlastreat.discord.redisbungee.RedisBungeeManager;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.platform.bungeecord.BungeeAudiences;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import org.bstats.bungeecord.Metrics;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

@Getter
@Setter
public class DiscordSyncBungee extends Plugin {

    @Getter
    private static DiscordSyncBungee instance;

    private DiscordCommon common;
    private Configuration config;
    private Metrics metrics;

    private BungeeAudiences adventure;

    public @NonNull BungeeAudiences adventure() {
        if(this.adventure == null) {
            throw new IllegalStateException("Cannot retrieve audience provider while plugin is not enabled");
        }
        return this.adventure;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        this.adventure = BungeeAudiences.create(this);

        getProxy().registerChannel(DiscordCommon.DATA_CHANNEL);
        getProxy().registerChannel(DiscordCommon.COMMAND_CHANNEL);

        String token = getConfig().getString("bot-token");
        String guildId = getConfig().getString("guild-id");
        boolean rename = getConfig().getBoolean("rename");

        List<DiscordRank> ranks = new ArrayList<>();

        for (String key : getConfig().getSection("ranks").getKeys()) {
            ranks.add(new DiscordRank(getConfig().getString("ranks." + key + ".id"), getConfig().getString("ranks." + key + ".permission")));
        }

        String db = getConfig().getString("database");
        DatabaseType databaseType = DatabaseType.getByName(db);
        Map<String, Object> credentials = new HashMap<>();
        switch (databaseType) {
            case SQL: {
                Configuration section = getConfig().getSection("sql");
                credentials.put("ip", section.getString("ip"));
                credentials.put("port", section.getInt("port"));
                credentials.put("user", section.getString("user"));
                credentials.put("password", section.getString("password"));
                credentials.put("database", section.getString("database"));
                credentials.put("table", section.getString("table"));
                credentials.put("properties", section.getString("properties"));
                credentials.put("driver", section.getString("driver", SqlHandler.DEFAULT_DRIVER));
                break;
            }
            case SQLITE: {
                Configuration section = getConfig().getSection("sqlite");
                credentials.put("file", section.getString("file"));
                credentials.put("driver", section.getString("driver", SQLiteHandler.DEFAULT_DRIVER));
                credentials.put("table", section.getString("table"));
                break;
            }
            case MONGODB: {
                Configuration section = getConfig().getSection("mongodb");
                credentials.put("url", section.getString("url"));
                credentials.put("database", section.getString("database"));
                credentials.put("collection", section.getString("collection"));
                break;
            }
        }
        boolean redisEnabled = getConfig().getBoolean("redis.enable", false);
        if (redisEnabled) {
            credentials.put("redis-host", getConfig().getString("redis.host"));
            credentials.put("redis-port", getConfig().getInt("redis.port"));
            credentials.put("redis-password", getConfig().getString("redis.password"));
        }

        Map<String, String> messages = new HashMap<>();
        Configuration messagesSection = getConfig().getSection("messages");
        for (String key : messagesSection.getKeys()) {
            messages.put(key, messagesSection.getString(key));
        }

        DiscordCommon.setLogger(new DefaultLogger(getLogger()));
        ProxyServer.getInstance().getScheduler().runAsync(this, () -> {
            DiscordCommon.getLogger().log(Level.INFO, "Configurating the bot...");
            try {
                common = new DiscordCommon(token, guildId, rename, databaseType, ranks, credentials, messages,
                        new BungeeOnlineCheck(),
                        new BungeeRunner(),
                        new BungeeConsoleExecutor(),
                        ServerType.BUNGEECORD,
                        config.getBoolean("one-time-reward", true),
                        config.getStringList("reward-command"),
                        config.getStringList("unlink-command"),
                        config.getBoolean("auto-unlink-on-leave", false),
                        config.getStringList("boost-reward"),
                        config.getString("date-format"),
                        new DiscordActivity(config.getBoolean("activity.enable", false), config.getString("activity.type", "PLAYING"),config.getString("activity.message", "DiscordRankSync")),
                        config.getInt("join-message-delay", 0),
                        config.getInt("refresh-delay", 30),
                        config.getInt("boost-delay", -1),
                        getDataFolder(),
                        redisEnabled);

                PluginManager pluginManager = ProxyServer.getInstance().getPluginManager();
                registerCommands(pluginManager);
                pluginManager.registerListener(this, new BungeeJoinListener(common));

                initMetrics();
            } catch (InterruptedException e) {
                DiscordCommon.getLogger().log(Level.SEVERE, "Failed to enable discord bot");
                e.printStackTrace();
            } finally {
                DiscordCommon.getLogger().log(Level.INFO, "Discord bot successfully enabled");
            }
            if (ProxyServer.getInstance().getPluginManager().getPlugin("RedisBungee") != null) {
                DiscordCommon.getLogger().log(Level.INFO, "RedisBungee detected, start working with...");
                RedisBungeeManager.init();
                ProxyServer.getInstance().getPluginManager().registerListener(this, new RedisBungeeListener());
            } else {
                ProxyServer.getInstance().getScheduler().schedule(this, () -> {
                    ProxyServer.getInstance().getScheduler().runAsync(this, () -> {
                        for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
                            DiscordBungeePluginMessage.sendData(player);
                        }
                    });
                }, common.getRefreshDelay(), TimeUnit.SECONDS);
            }
        });
    }

    private void registerCommands(PluginManager pluginManager) {
        pluginManager.registerCommand(this, new BungeeLinkCommand(common));
        pluginManager.registerCommand(this, new BungeeUnlinkCommand(common));
        pluginManager.registerCommand(this, new BungeeClaimBoostCommand(common));
        pluginManager.registerCommand(this, new BungeeStopbotCommand(common));
    }

    private void initMetrics() {
        metrics = new Metrics(this, DiscordCommon.METRICS_ID);
        metrics.addCustomChart(new AllTimeLinkCountChart());
        metrics.addCustomChart(new DiscordCountChart());
        metrics.addCustomChart(new DiscordOnlineChart());
        metrics.addCustomChart(new LinkCountChart());
        metrics.addCustomChart(new RankCountChart());
        metrics.addCustomChart(new RenameChart());
        metrics.addCustomChart(new ServerTypeChart());
    }

    @Override
    public void onDisable() {
        if(this.adventure == null) return;
        this.adventure.close();
        this.adventure = null;
    }

    private void saveDefaultConfig() {
        if (!getDataFolder().exists())
            getDataFolder().mkdir();
        File file = new File(getDataFolder(), "config.yml");
        try {
            if (!file.exists())
                Files.copy(getResourceAsStream("config.yml"), file.toPath(), new java.nio.file.CopyOption[0]);
            this.config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
