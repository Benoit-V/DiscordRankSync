package fr.bebedlastreat.discord.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginManager;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import fr.bebedlastreat.discord.common.DiscordCommon;
import fr.bebedlastreat.discord.common.charts.*;
import fr.bebedlastreat.discord.common.enums.DatabaseType;
import fr.bebedlastreat.discord.common.enums.ServerType;
import fr.bebedlastreat.discord.common.logger.VelocityLogger;
import fr.bebedlastreat.discord.common.objects.DiscordActivity;
import fr.bebedlastreat.discord.common.objects.DiscordRank;
import fr.bebedlastreat.discord.common.sql.SqlHandler;
import fr.bebedlastreat.discord.common.sqlite.SQLiteHandler;
import fr.bebedlastreat.discord.redisbungee.RedisBungeeManager;
import fr.bebedlastreat.discord.velocity.commands.VelocityClaimBoostCommand;
import fr.bebedlastreat.discord.velocity.commands.VelocityLinkCommand;
import fr.bebedlastreat.discord.velocity.commands.VelocityStopbotCommand;
import fr.bebedlastreat.discord.velocity.commands.VelocityUnlinkCommand;
import fr.bebedlastreat.discord.velocity.implementations.VelocityConsoleExecutor;
import fr.bebedlastreat.discord.velocity.implementations.VelocityOnlineCheck;
import fr.bebedlastreat.discord.velocity.implementations.VelocityRunner;
import fr.bebedlastreat.discord.velocity.listeners.RedisBungeeListener;
import fr.bebedlastreat.discord.velocity.listeners.VelocityJoinListener;
import fr.bebedlastreat.discord.velocity.squishyyaml.ConfigurationSection;
import fr.bebedlastreat.discord.velocity.squishyyaml.YamlConfiguration;
import fr.bebedlastreat.discord.velocity.utils.DiscordVelocityPluginMessage;
import lombok.Getter;
import lombok.Setter;
import org.bstats.velocity.Metrics;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

@Plugin(
        id = "discordranksync",
        name = "DiscordRankSync",
        version = "${project.version}",
        authors = "BebeDlaStreat",
        description = "Allow user to link their discord to their minecraft account",
        dependencies = {@Dependency(id = "redisbungee", optional = true)}
)
@Getter
@Setter
public class DiscordSyncVelocity {
    @Getter
    private static DiscordSyncVelocity instance;
    private DiscordCommon common;
    private Metrics metrics;
    private YamlConfiguration config;


    @Getter
    private static ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private final Metrics.Factory metricsFactory;

    @Inject
    public DiscordSyncVelocity(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory, Metrics.Factory metricsFactory) {
        DiscordSyncVelocity.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.metricsFactory = metricsFactory;
    }

    @Subscribe
    public void onEnable(ProxyInitializeEvent event) {
        instance = this;
        saveDefaultConfig();

        server.getChannelRegistrar().register(MinecraftChannelIdentifier.from(DiscordCommon.DATA_CHANNEL));
        server.getChannelRegistrar().register(MinecraftChannelIdentifier.from(DiscordCommon.COMMAND_CHANNEL));

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
                ConfigurationSection section = getConfig().getSection("sql");
                credentials.put("ip", section.getString("ip"));
                credentials.put("port", section.getInteger("port"));
                credentials.put("user", section.getString("user"));
                credentials.put("password", section.getString("password"));
                credentials.put("database", section.getString("database"));
                credentials.put("table", section.getString("table"));
                credentials.put("properties", section.getString("properties"));
                credentials.put("driver", section.getString("driver", SqlHandler.DEFAULT_DRIVER));
                break;
            }
            case SQLITE: {
                ConfigurationSection section = getConfig().getSection("sqlite");
                credentials.put("file", section.getString("file"));
                credentials.put("driver", section.getString("driver", SQLiteHandler.DEFAULT_DRIVER));
                credentials.put("table", section.getString("table"));
                break;
            }
            case MONGODB: {
                ConfigurationSection section = getConfig().getSection("mongodb");
                credentials.put("url", section.getString("url"));
                credentials.put("database", section.getString("database"));
                credentials.put("collection", section.getString("collection"));
                break;
            }
        }
        boolean redisEnabled = getConfig().getBoolean("redis.enable", false);
        if (redisEnabled) {
            credentials.put("redis-host", getConfig().getString("redis.host"));
            credentials.put("redis-port", getConfig().getInteger("redis.port"));
            credentials.put("redis-password", getConfig().getString("redis.password"));
        }

        Map<String, String> messages = new HashMap<>();
        ConfigurationSection messagesSection = getConfig().getSection("messages");
        for (String key : messagesSection.getKeys()) {
            messages.put(key, messagesSection.getString(key));
        }

        DiscordCommon.setLogger(new VelocityLogger(logger));
        server.getScheduler().buildTask(this, () -> {
            DiscordCommon.getLogger().log(Level.INFO, "Configurating the bot...");
            try {
                common = new DiscordCommon(token, guildId, rename, databaseType, ranks, credentials, messages,
                        new VelocityOnlineCheck(),
                        new VelocityRunner(),
                        new VelocityConsoleExecutor(),
                        ServerType.VELOCITY,
                        config.getBoolean("one-time-reward", true),
                        config.getListString("reward-command"),
                        config.getListString("unlink-command"),
                        config.getBoolean("auto-unlink-on-leave", false),
                        config.getListString("boost-reward"),
                        config.getString("date-format"),
                        new DiscordActivity(config.getBoolean("activity.enable", false), config.getString("activity.type", "PLAYING"), config.getString("activity.message", "DiscordRankSync")),
                        config.getInteger("join-message-delay", 0),
                        config.getInteger("refresh-delay", 30),
                        config.getInteger("boost-delay", -1),
                        getDataDirectory().toFile(),
                        redisEnabled);

                EventManager eventManager = server.getEventManager();
                CommandManager commandManager = server.getCommandManager();
                registerCommands(commandManager);
                eventManager.register(this, new VelocityJoinListener(common));

                initMetrics();
            } catch (InterruptedException e) {
                DiscordCommon.getLogger().log(Level.SEVERE, "Failed to enable discord bot");
                e.printStackTrace();
            } finally {
                DiscordCommon.getLogger().log(Level.INFO, "Discord bot successfully enabled");
            }
            if (server.getPluginManager().getPlugin("redisbungee").isPresent()) {
                DiscordCommon.getLogger().log(Level.INFO, "RedisBungee detected, start working with...");
                RedisBungeeManager.init();
                server.getEventManager().register(this, new RedisBungeeListener());
            } else {
                server.getScheduler().buildTask(this, () -> {
                    for (Player player : server.getAllPlayers()) {
                        DiscordVelocityPluginMessage.sendData(player);
                    }
                }).repeat(common.getRefreshDelay(), TimeUnit.SECONDS).schedule();
            }
        }).schedule();
    }

    private void registerCommands(CommandManager commandManager) {
        CommandMeta link = commandManager.metaBuilder("link").build();
        commandManager.register(link, new VelocityLinkCommand(common));
        CommandMeta unlink = commandManager.metaBuilder("unlink").build();
        commandManager.register(unlink, new VelocityUnlinkCommand(common));
        CommandMeta claimBoost = commandManager.metaBuilder("claimboost").build();
        commandManager.register(claimBoost, new VelocityClaimBoostCommand(common));
        CommandMeta stopbot = commandManager.metaBuilder("stopbot").aliases("stopdiscord").build();
        commandManager.register(stopbot, new VelocityStopbotCommand(common));
    }

    private void initMetrics() {
        metrics = metricsFactory.make(this, DiscordCommon.METRICS_ID);
        metrics.addCustomChart(new AllTimeLinkCountChart());
        metrics.addCustomChart(new DiscordCountChart());
        metrics.addCustomChart(new DiscordOnlineChart());
        metrics.addCustomChart(new LinkCountChart());
        metrics.addCustomChart(new RankCountChart());
        metrics.addCustomChart(new RenameChart());
        metrics.addCustomChart(new ServerTypeChart());
    }

    private void saveDefaultConfig() {
        File dataFile = dataDirectory.toFile();
        if (!dataFile.exists())
            dataFile.mkdir();
        File file = new File(dataFile, "config.yml");
        this.config = new YamlConfiguration(file);
        config.load();
    }
}
