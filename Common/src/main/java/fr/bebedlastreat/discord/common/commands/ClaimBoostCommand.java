package fr.bebedlastreat.discord.common.commands;

import fr.bebedlastreat.discord.common.DiscordCommon;
import fr.bebedlastreat.discord.common.interfaces.ICommonCommand;
import fr.bebedlastreat.discord.common.interfaces.ICommonCommandSender;
import fr.bebedlastreat.discord.common.interfaces.ICommonPlayer;
import fr.bebedlastreat.discord.common.objects.DiscordMember;

import java.time.YearMonth;
import java.util.Date;

public class ClaimBoostCommand implements ICommonCommand {

    private final DiscordCommon common;

    public ClaimBoostCommand(DiscordCommon common) {
        this.common = common;
    }

    @Override
    public void execute(ICommonPlayer<?> player, String[] args) {
        if (common.getBoostReward().isEmpty()) return;
        common.getRunner().runAsync(() -> {
            String discord = common.getDatabaseFetch().discord(player.getUniqueId());
            if (discord.isEmpty()) {
                common.getRunner().run(() -> {
                    player.sendMessage(common.getMessages().get("not-linked"));
                });
                return;
            }
            DiscordMember member = common.getMember(discord);
            if (member != null && member.isBoosting()) {
                if (common.getDatabaseFetch().canBoost(player.getUniqueId())) {
                    common.getDatabaseFetch().deleteBoost(player.getUniqueId());
                    int days = common.getBoostDelay() == -1 ? YearMonth.now().lengthOfMonth() : common.getBoostDelay();
                    common.getDatabaseFetch().insertBoost(player.getUniqueId(), System.currentTimeMillis() + (1000L * 60 * 60 * 24 * days));
                    common.getRunner().runLater(() -> {
                        for (String command : common.getBoostReward()) {
                            common.getConsoleExecutor().execute(command.replace("{player}", player.getName()).replace("{uuid}", player.getUniqueId().toString()), player);
                        }
                    }, 0);

                    common.getRunner().run(() -> {
                        player.sendMessage(common.getMessages().get("boost-claim"));
                    });
                } else {
                    String date = common.getSdf().format(new Date(common.getDatabaseFetch().getNextBoost(player.getUniqueId())));
                    common.getRunner().run(() -> {
                        player.sendMessage(common.getMessages().get("boost-countdown").replace("{date}", date));
                    });
                }
            } else {
                common.getRunner().run(() -> {
                    player.sendMessage(common.getMessages().get("not-boosting"));
                });
            }
        });
    }

    @Override
    public void execute(ICommonCommandSender<?> sender, String[] args) {

    }
}
