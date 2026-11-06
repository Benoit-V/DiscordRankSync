package fr.bebedlastreat.discord.common.charts;

import fr.bebedlastreat.discord.common.DiscordCommon;
import org.bstats.charts.SingleLineChart;

public class DiscordOnlineChart extends SingleLineChart {
    public DiscordOnlineChart() {
        super("discord_online", () -> {
            return DiscordCommon.getInstance().retrieveMemberCount(true);
        });
    }
}
