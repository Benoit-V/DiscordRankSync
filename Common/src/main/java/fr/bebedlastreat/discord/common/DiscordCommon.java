package fr.bebedlastreat.discord.common;

import fr.bebedlastreat.discord.common.commands.ClaimBoostCommand;
import fr.bebedlastreat.discord.common.commands.LinkCommand;
import fr.bebedlastreat.discord.common.commands.StopbotCommand;
import fr.bebedlastreat.discord.common.commands.UnlinkCommand;
import fr.bebedlastreat.discord.common.enums.DatabaseType;
import fr.bebedlastreat.discord.common.enums.ServerType;
import fr.bebedlastreat.discord.common.interfaces.*;
import fr.bebedlastreat.discord.common.listeners.GuildMemberListener;
import fr.bebedlastreat.discord.common.listeners.JoinListener;
import fr.bebedlastreat.discord.common.listeners.SlashCommandListener;
import fr.bebedlastreat.discord.common.logger.DefaultLogger;
import fr.bebedlastreat.discord.common.logger.IDiscordLogger;
import fr.bebedlastreat.discord.common.mongodb.MongoDBFetch;
import fr.bebedlastreat.discord.common.mongodb.MongoDBHandler;
import fr.bebedlastreat.discord.common.objects.DiscordActivity;
import fr.bebedlastreat.discord.common.objects.DiscordMember;
import fr.bebedlastreat.discord.common.objects.DiscordRank;
import fr.bebedlastreat.discord.common.objects.WaitingLink;
import fr.bebedlastreat.discord.common.redis.RedisHandler;
import fr.bebedlastreat.discord.common.sql.SqlCredentials;
import fr.bebedlastreat.discord.common.sql.SqlFetch;
import fr.bebedlastreat.discord.common.sql.SqlHandler;
import fr.bebedlastreat.discord.common.sqlite.SQLiteCredentials;
import fr.bebedlastreat.discord.common.sqlite.SQLiteFetch;
import fr.bebedlastreat.discord.common.sqlite.SQLiteHandler;
import fr.bebedlastreat.discord.redisbungee.RedisBungeeManager;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

@Data
public class DiscordCommon {

    @Getter
    private static DiscordCommon instance;
    public static final int METRICS_ID = 19271;
    public static final String DATA_CHANNEL = "discordrs:data";
    public static final String COMMAND_CHANNEL = "discordrs:command";

    private final String token;
    private final String guildId;
    private final boolean rename;
    private final JDA jda;
    private final Guild guild;
    private final DatabaseType databaseType;
    private final Map<String, Object> credentials;
    private final Map<String, String> messages;
    private final IDatabaseFetch databaseFetch;
    private IOnlineCheck onlineCheck;
    private final Map<String, WaitingLink> waitingLinks = new HashMap<>();
    private final List<DiscordRank> ranks;
    private final ICommonRunner runner;
    private final IConsoleExecutor consoleExecutor;
    private final LinkCommand linkCommand;
    private final UnlinkCommand unlinkCommand;
    private final ClaimBoostCommand claimBoostCommand;
    private final StopbotCommand stopbotCommand;
    private final JoinListener joinListener;
    private final ServerType serverType;
    private int linkCount = 0;
    private int allTimeLinkCount = 0;
    private final boolean oneTimeReward;
    private final List<String> rewardCommand;
    private final List<String> unlinkCommandList;
    private final boolean autoUnlinkOnLeave;
    private final List<String> boostReward;
    private final SimpleDateFormat sdf;
    private final DiscordActivity activity;
    private final int joinMessageDelay;
    private final int refreshDelay;
    private final int boostDelay;
    private final boolean redisEnabled;
    private final RedisHandler redisHandler;

    private boolean standalone = false;
    private boolean redisBungee = false;

    @Getter
    @Setter
    private static IDiscordLogger logger = new DefaultLogger(Logger.getLogger("DiscordRankSync"));

    public DiscordCommon(String token,
                         String guildId,
                         boolean rename,
                         DatabaseType databaseType,
                         List<DiscordRank> ranks,
                         Map<String, Object> credentials,
                         Map<String, String> messages,
                         IOnlineCheck onlineCheck,
                         ICommonRunner runner,
                         IConsoleExecutor consoleExecutor,
                         ServerType serverType,
                         boolean oneTimeReward,
                         List<String> rewardCommand,
                         List<String> unlinkCommandList,
                         boolean autoUnlinkOnLeave,
                         List<String> boostReward,
                         String dataFormat,
                         DiscordActivity activity,
                         int joinMessageDelay,
                         int refreshDelay,
                         int boostDelay,
                         File dataFolder,
                         boolean redisEnabled
    ) throws InterruptedException {
        this.token = token;
        this.guildId = guildId;
        this.rename = rename;
        this.messages = messages;
        this.onlineCheck = onlineCheck;
        this.runner = runner;
        this.consoleExecutor = consoleExecutor;
        this.serverType = serverType;
        this.oneTimeReward = oneTimeReward;
        this.rewardCommand = rewardCommand;
        this.unlinkCommandList = unlinkCommandList;
        this.autoUnlinkOnLeave = autoUnlinkOnLeave;
        this.boostReward = boostReward;
        this.sdf = new SimpleDateFormat(dataFormat);
        this.activity = activity;
        this.joinMessageDelay = joinMessageDelay;
        this.refreshDelay = Math.min(1, refreshDelay);
        this.boostDelay = Math.min(1, boostDelay);
        instance = this;
        this.databaseType = databaseType;
        this.credentials = credentials;
        this.ranks = ranks;
        this.linkCommand = new LinkCommand(this);
        this.unlinkCommand = new UnlinkCommand(this);
        this.claimBoostCommand = new ClaimBoostCommand(this);
        this.stopbotCommand = new StopbotCommand(this);
        this.joinListener = new JoinListener(this);

        switch (databaseType) {
            case SQL: {
                SqlHandler sqlHandler = new SqlHandler(
                        new SqlCredentials((String) credentials.get("ip"), (String) credentials.get("user"), (String) credentials.get("password"), (String) credentials.get("database"), (int) credentials.get("port"), (String) credentials.get("properties"), (String) credentials.get("driver")),
                        10, 10, 1800000, 0, 5000, (String) credentials.get("table")
                );
                sqlHandler.createDefault();
                databaseFetch = new SqlFetch(sqlHandler);
                break;
            }
            case SQLITE: {
                SQLiteHandler sqLiteHandler = new SQLiteHandler(
                        new SQLiteCredentials(new File(dataFolder, (String) credentials.get("file")), (String) credentials.get("driver")),
                        (String) credentials.get("table"));
                sqLiteHandler.createDefault();
                databaseFetch = new SQLiteFetch(sqLiteHandler);
                break;
            }
            case MONGODB: {
                MongoDBHandler mongoDBHandler = new MongoDBHandler((String) credentials.get("url"), (String) credentials.get("database"), (String) credentials.get("collection"));
                mongoDBHandler.initConnection();

                databaseFetch = new MongoDBFetch(mongoDBHandler);
                break;
            }
            default: {
                databaseFetch = null;
                break;
            }
        }

        if (databaseFetch == null) {
            throw new RuntimeException("No database storage found, please verify you properly configured the plugin");
        }

        this.redisEnabled = redisEnabled;
        if (redisEnabled) {
            redisHandler = new RedisHandler((String) credentials.get("redis-host"), (int) credentials.get("redis-port"), (String) credentials.get("redis-password"));
            redisHandler.init();
        } else {
            redisHandler = null;
        }

        this.jda = JDABuilder.createDefault(token)
                .enableIntents(GatewayIntent.GUILD_MEMBERS)
                .setMemberCachePolicy(MemberCachePolicy.BOOSTER)
                .setAutoReconnect(true)
                .addEventListeners(new SlashCommandListener(this), new GuildMemberListener(this))
                .build();
        if (activity.isEnable()) {
            jda.getPresence().setActivity(Activity.of(activity.getActivityType(), activity.getMessage()));
        }
        jda.upsertCommand("ping", messages.get("ping-command"))
                .queue(command -> logger.log(Level.INFO, command.getName() + " command added"));

        jda.upsertCommand("link", messages.get("link-command"))
                .addOption(OptionType.STRING, messages.get("name"), messages.get("link-command-name"), true)
                .queue(command -> logger.log(Level.INFO, command.getName() + " command added"));

        jda.upsertCommand("unlink", messages.get("unlink-command"))
                .queue(command -> logger.log(Level.INFO, command.getName() + " command added"));

        jda.upsertCommand("minecraft", messages.get("minecraft-command"))
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                .addOption(OptionType.USER, messages.get("name"), messages.get("minecraft-command-name"), true)
                .queue(command -> logger.log(Level.INFO, command.getName() + " command added"));

        jda.upsertCommand("discord", messages.get("discord-command"))
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                .addOption(OptionType.STRING, messages.get("name"), messages.get("discord-command-name"), true)
                .queue(command -> logger.log(Level.INFO, command.getName() + " command added"));
        jda.awaitReady();
        guild = jda.getGuildById(guildId);

        for (DiscordRank rank : ranks) {
            Role role = guild.getRoleById(rank.getDiscordId());
            if (role == null) {
                logger.log(Level.WARNING, "Rank " + rank.getDiscordId() + " is null");
                continue;
            }
            rank.setRole(role);
        }
        linkCount = databaseFetch.count();
        allTimeLinkCount = databaseFetch.allTimeCount();

        runner.runTask(() -> {
            if (redisBungee) {
                RedisBungeeManager.updateMainProxy();
            }
        }, 100, 100);
    }

    public void addRole(String discordId, DiscordRank rank) {
        if (standalone) {
            if (redisBungee) {
                RedisBungeeManager.addRole(discordId, rank);
            }
            return;
        }
        if (rank.getRole() == null) return;
        Member member = guild.retrieveMemberById(discordId).onErrorMap((error) -> null).complete();
        if (member == null) return;
        if (member.getRoles().contains(rank.getRole())) return;
        try {
            guild.addRoleToMember(member, rank.getRole()).queue(
                    (success) -> {

                    }, (throwable) -> {
                        logger.log(Level.WARNING, "error when adding roles of " + discordId + ": " + throwable.getMessage());
                    }
            );
        } catch (InsufficientPermissionException | HierarchyException ex) {
            if (ex instanceof InsufficientPermissionException) {
                logger.log(Level.WARNING, "the bot has not the permission to manage roles");
            }
        }
    }

    public void removeRole(String discordId, DiscordRank rank) {
        if (standalone) {
            if (redisBungee) {
                RedisBungeeManager.removeRole(discordId, rank);
            }
            return;
        }
        if (rank.getRole() == null) return;
        Member member = guild.retrieveMemberById(discordId).onErrorMap((error) -> null).complete();
        if (member == null) return;
        if (!member.getRoles().contains(rank.getRole())) return;
        try {
            guild.removeRoleFromMember(member, rank.getRole()).queue(
                    (success) -> {

                    }, (throwable) -> {
                        logger.log(Level.WARNING, "error when removing roles of " + discordId + ": " + throwable.getMessage());
                    }
            );
        } catch (InsufficientPermissionException | HierarchyException ex) {
            if (ex instanceof InsufficientPermissionException) {
                logger.log(Level.WARNING, "the bot has not the permission to manage roles");
            }
        }
    }

    public void unlinkByDiscordId(String discordId, boolean removeRoles) {
        if (!databaseFetch.exist(discordId)) {
            return;
        }

        try {
            String uuid = databaseFetch.uuid(discordId);
            String name = databaseFetch.name(UUID.fromString(uuid));

            if (!unlinkCommandList.isEmpty()) {
                runner.runLater(() -> {
                    for (String command : unlinkCommandList) {
                        consoleExecutor.execute(command.replace("{player}", name).replace("{uuid}", uuid), null);
                    }
                }, 0);
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Can't fetch minecraft user data from discord user " + discordId);
            ex.printStackTrace();
        }

        databaseFetch.delete(discordId);
        linkCount--;

        if (removeRoles) {
            runner.runAsync(() -> {
                for (DiscordRank rank : ranks) {
                    removeRole(discordId, rank);
                }
            });
        }
    }

    public void rename(String discordId, String name) {
        if (standalone) {
            if (redisBungee) {
                RedisBungeeManager.rename(discordId, name);
            }
            return;
        }
        if (!rename) return;
        if (guild == null) return;
        Member member = guild.retrieveMemberById(discordId).onErrorMap((error) -> null).complete();
        if (member == null) return;
        try {
            member.modifyNickname(name).queue(
                    (success) -> {

                    }, (throwable) -> {
                        logger.log(Level.WARNING, "error when renaming " + discordId + ": " + throwable.getMessage());
                    }
            );
        } catch (InsufficientPermissionException | HierarchyException ex) {
            if (ex instanceof InsufficientPermissionException) {
                logger.log(Level.WARNING, "the bot has not the permission to rename a user");
            } else {
                logger.log(Level.WARNING, "the bot can't rename " + member.getUser().getName());
            }
        }
    }

    public DiscordMember getMember(String id) {
        try {
            Member member = guild.retrieveMemberById(id).onErrorMap((error) -> null).complete();
            if (member == null) return null;
            return new DiscordMember(id, member.getEffectiveName(), member.isBoosting());
        } catch (Exception ex) {
            return null;
        }
    }

    public void getData(ICommonPlayer<?> player, PlayerDataCallback callback) {
        String discord = DiscordCommon.getInstance().getDatabaseFetch().discord(player.getUniqueId());
        boolean linked = discord != null && !discord.isEmpty();
        String name = "none";
        boolean boosting = false;
        if (linked) {
            DiscordMember member = DiscordCommon.getInstance().getMember(discord);
            if (member != null) {
                name = member.getEffectiveName();
                boosting = member.isBoosting();
            }
        }
        callback.execute(linked, name, boosting);
    }

    public interface PlayerDataCallback {
        void execute(boolean linked, String discordName, boolean boosting);
    }

}
