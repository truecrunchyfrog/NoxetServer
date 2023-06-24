package org.noxet.noxetserver.commands.debug;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.noxet.noxetserver.messaging.ErrorMessage;
import org.noxet.noxetserver.messaging.SuccessMessage;
import org.noxet.noxetserver.playerdata.PlayerDataManager;

public class ClearPlayerDataCache implements CommandExecutor {
    @Override
    @SuppressWarnings("NullableProblems")
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(!commandSender.isOp()) {
            new ErrorMessage(ErrorMessage.ErrorType.PERMISSION, "Only operators can clear player data cache.").send(commandSender);
            return true;
        }

        PlayerDataManager.clearAllCache();

        new SuccessMessage("Cache for all player data was cleared.").send(commandSender);

        return true;
    }
}
