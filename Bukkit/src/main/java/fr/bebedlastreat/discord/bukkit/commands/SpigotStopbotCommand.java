package fr.bebedlastreat.discord.bukkit.commands;

import fr.bebedlastreat.discord.bukkit.implementations.SpigotCommandSender;
import fr.bebedlastreat.discord.common.DiscordCommon;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class SpigotStopbotCommand implements CommandExecutor {

    private final DiscordCommon common;

    public SpigotStopbotCommand(DiscordCommon common) {
        this.common = common;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        common.getStopbotCommand().execute(new SpigotCommandSender(sender), args);
        return true;
    }
}
