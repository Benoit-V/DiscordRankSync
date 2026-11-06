package fr.bebedlastreat.discord.common.charts;

import fr.bebedlastreat.discord.common.DiscordCommon;
import org.bstats.charts.SingleLineChart;

public class DiscordCountChart extends SingleLineChart {
    public DiscordCountChart() {
        super("discord_count", () -> {
            return DiscordCommon.getInstance().retrieveMemberCount(false);
        });
    }
}
