package fr.bebedlastreat.discord.common.commands;

import fr.bebedlastreat.discord.common.DiscordCommon;
import fr.bebedlastreat.discord.common.interfaces.ICommonCommand;
import fr.bebedlastreat.discord.common.interfaces.ICommonCommandSender;
import fr.bebedlastreat.discord.common.interfaces.ICommonPlayer;

public class StopbotCommand implements ICommonCommand {

    private final DiscordCommon common;

    public StopbotCommand(DiscordCommon common) {
        this.common = common;
    }
    @Override
    public void execute(ICommonPlayer<?> player, String[] args) {

    }

    @Override
    public void execute(ICommonCommandSender<?> sender, String[] args) {
        common.stopBot();
        sender.sendMessage(common.getMessages().get("stop-bot"));
    }
}
