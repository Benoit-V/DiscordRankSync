package fr.bebedlastreat.discord.redisbungee;

import com.google.gson.Gson;
import com.imaginarycode.minecraft.redisbungee.RedisBungeeAPI;
import com.imaginarycode.minecraft.redisbungee.api.events.IPubSubMessageEvent;
import fr.bebedlastreat.discord.common.DiscordCommon;
import fr.bebedlastreat.discord.common.objects.DiscordRank;
import fr.bebedlastreat.discord.common.objects.WaitingLink;
import fr.bebedlastreat.discord.redisbungee.data.*;
import fr.bebedlastreat.discord.redisbungee.implementations.RedisBungeeOnlineCheck;
import lombok.Getter;
import lombok.experimental.UtilityClass;

import java.util.logging.Level;

@UtilityClass
public class RedisBungeeManager {

    private RedisBungeeAPI redisBungeeAPI;
    public final String CHANNEL = "DiscordRankSync";
    private final Gson gson = new Gson();
    private DiscordCommon common;

    @Getter
    public boolean mainProxy;

    public void init() {
        redisBungeeAPI = RedisBungeeAPI.getRedisBungeeApi();
        redisBungeeAPI.registerPubSubChannels(CHANNEL);
        common = DiscordCommon.getInstance();
        updateMainProxy();
        if (isMainProxy()) {
            DiscordCommon.getLogger().log(Level.INFO, "It seems that i'm the first proxy, so i will handle the discord bot");
        } else {
            DiscordCommon.getLogger().log(Level.INFO, "I'm not the main proxy, I will not handle the discord bot");
        }
        common.setRedisBungee(true);
        common.setOnlineCheck(new RedisBungeeOnlineCheck());
    }

    public void handlePubSub(IPubSubMessageEvent event) {
        if (event.getChannel().equalsIgnoreCase(CHANNEL)) {
            PubSubMessage pubSubMessage = gson.fromJson(event.getMessage(), PubSubMessage.class);
            switch (pubSubMessage.getMessageType()) {
                case ADD_ROLE:
                case REMOVE_ROLE: {
                    if (isMainProxy()) {
                        RoleData data = gson.fromJson(gson.toJsonTree(pubSubMessage.getContent()), RoleData.class);
                        for (DiscordRank rank : common.getRanks()) {
                            if (rank.getDiscordId().equalsIgnoreCase(data.getRankId())) {
                                if (pubSubMessage.getMessageType() == MessageType.ADD_ROLE) {
                                    common.addRole(data.getDiscordId(), rank);
                                } else {
                                    common.removeRole(data.getDiscordId(), rank);
                                }
                                break;
                            }
                        }
                    }
                    break;
                }
                case RENAME: {
                    if (isMainProxy()) {
                        RenameData data = gson.fromJson(gson.toJsonTree(pubSubMessage.getContent()), RenameData.class);
                        common.rename(data.getDiscordId(), data.getName());
                    }
                    break;
                }
                case ADD_WAITING_LINK: {
                    WaitingLinkData data = gson.fromJson(gson.toJsonTree(pubSubMessage.getContent()), WaitingLinkData.class);
                    if (!data.getProxy().equals(redisBungeeAPI.getProxyId())) {
                        common.getWaitingLinks().put(data.getId(), data.getWaitingLink());
                    }
                    break;
                }
                case REMOVE_WAITING_LINK: {
                    WaitingLinkData data = gson.fromJson(gson.toJsonTree(pubSubMessage.getContent()), WaitingLinkData.class);
                    if (!data.getProxy().equals(redisBungeeAPI.getProxyId())) {
                        common.getWaitingLinks().remove(data.getId());
                    }
                    break;
                }
            }
        }
    }

    public void sendPubSub(String channel, String message) {
        redisBungeeAPI.sendChannelMessage(channel, message);
    }

    public void updateMainProxy() {
        boolean oldState = mainProxy;
        mainProxy = redisBungeeAPI.getAllProxies().indexOf(redisBungeeAPI.getProxyId()) == 0;
        common.setStandalone(!mainProxy);
        if (oldState != mainProxy) {
            if (mainProxy) {
                DiscordCommon.getLogger().log(Level.INFO, "I'm now the main proxy");
            } else {
                DiscordCommon.getLogger().log(Level.INFO, "I'm no more the main proxy");
            }
        }
    }

    public void addRole(String discordId, DiscordRank rank) {
        sendPubSub(CHANNEL, gson.toJson(new PubSubMessage(MessageType.ADD_ROLE, new RoleData(discordId, rank))));
    }


    public void removeRole(String discordId, DiscordRank rank) {
        sendPubSub(CHANNEL, gson.toJson(new PubSubMessage(MessageType.REMOVE_ROLE, new RoleData(discordId, rank))));
    }


    public void rename(String discordId, String name) {
        sendPubSub(CHANNEL, gson.toJson(new PubSubMessage(MessageType.RENAME, new RenameData(discordId, name))));
    }

    public void addWaitingLink(String id, WaitingLink waitingLink) {
        sendPubSub(CHANNEL, gson.toJson(new PubSubMessage(MessageType.ADD_WAITING_LINK, new WaitingLinkData(redisBungeeAPI.getProxyId(), id, waitingLink))));
    }

    public void removeWaitingLink(String id) {
        sendPubSub(CHANNEL, gson.toJson(new PubSubMessage(MessageType.REMOVE_WAITING_LINK, new WaitingLinkData(redisBungeeAPI.getProxyId(), id, null))));
    }

}
