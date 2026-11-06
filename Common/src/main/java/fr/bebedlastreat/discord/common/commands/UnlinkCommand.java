package fr.bebedlastreat.discord.common.commands;

import fr.bebedlastreat.discord.common.DiscordCommon;
import fr.bebedlastreat.discord.common.interfaces.ICommonCommand;
import fr.bebedlastreat.discord.common.interfaces.ICommonCommandSender;
import fr.bebedlastreat.discord.common.interfaces.ICommonPlayer;

import java.util.Collections;

public class UnlinkCommand implements ICommonCommand {

    private final DiscordCommon common;

    public UnlinkCommand(DiscordCommon common) {
        this.common = common;
    }

    @Override
    public void execute(ICommonPlayer<?> player, String[] args) {
        common.getRunner().runAsync(() -> {
            String discord = common.getDatabaseFetch().discord(player.getUniqueId());
            if (discord.isEmpty() || !common.getDatabaseFetch().delete(player.getUniqueId())) {
                common.getRunner().run(() -> {
                    player.sendMessage(common.getMessages().get("not-linked"));
                });
                return;
            }
            common.decrementLinkCount();
            common.syncRoles(discord, Collections.emptyList(), common.getRanks());

            if (!common.getUnlinkCommandList().isEmpty()) {
                common.getRunner().runLater(() -> {
                    for (String command : common.getUnlinkCommandList()) {
                        common.getConsoleExecutor().execute(command.replace("{player}", player.getName()).replace("{uuid}", player.getUniqueId().toString()), player);
                    }
                }, 0);
            }

            common.getRunner().run(() -> {
                player.sendMessage(common.getMessages().get("unlink-success"));
            });
        });
    }

    @Override
    public void execute(ICommonCommandSender<?> sender, String[] args) {

    }
}
