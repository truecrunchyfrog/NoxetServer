package org.noxet.noxetserver.commands.misc;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.noxet.noxetserver.messaging.NoxetErrorMessage;
import org.noxet.noxetserver.messaging.NoxetSuccessMessage;
import org.noxet.noxetserver.playerdata.PlayerDataManager;

public class ClearPlayerDataCache implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(!commandSender.isOp()) {
            new NoxetErrorMessage(NoxetErrorMessage.ErrorType.PERMISSION, "Only operators can clear player data cache.").send(commandSender);
            return true;
        }

        PlayerDataManager.clearAllCache();

        new NoxetSuccessMessage("Cache for all player data was cleared.").send(commandSender);

        return true;
    }
}
