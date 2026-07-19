package fr.bebedlastreat.discord.common.listeners;

import fr.bebedlastreat.discord.common.DiscordCommon;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.logging.Level;

public class GuildMemberListener extends ListenerAdapter {

    private final DiscordCommon common;

    public GuildMemberListener(DiscordCommon common) {
        this.common = common;
    }

    @Override
    public void onGuildMemberRemove(GuildMemberRemoveEvent event) {
        if (common.isStandalone()) return;
        if (!common.isAutoUnlinkOnLeave()) return;
        if (!event.getGuild().getId().equals(common.getGuildId())) return;

        String discordId = event.getUser().getId();
        common.getRunner().runAsync(() -> {
            if (!common.getDatabaseFetch().exist(discordId)) {
                return;
            }
            common.unlinkByDiscordId(discordId, false);
            DiscordCommon.getLogger().log(Level.INFO, "Automatically unlinked " + discordId + " after leaving the Discord server");
        });
    }
}
