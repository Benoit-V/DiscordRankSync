package fr.bebedlastreat.discord.bungeecord.commands;

import fr.bebedlastreat.discord.bungeecord.implementations.BungeeCommandSender;
import fr.bebedlastreat.discord.common.DiscordCommon;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

public class BungeeStopbotCommand extends Command {

    private final DiscordCommon common;

    public BungeeStopbotCommand(DiscordCommon common) {
        super("stopbot", "discord.stopbot", "stopdiscord");
        this.common = common;

    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        common.getStopbotCommand().execute(new BungeeCommandSender(sender), args);
    }
}
