package org.noxet.noxetserver.commands.games.creepersweeper;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.noxet.noxetserver.menus.inventory.CreeperSweeperGameMenu;
import org.noxet.noxetserver.messaging.ErrorMessage;

public class CreeperSweeper implements CommandExecutor {
    @Override
    @SuppressWarnings("NullableProblems")
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(!(commandSender instanceof Player)) {
            new ErrorMessage(ErrorMessage.ErrorType.COMMON, "Only players can play Creeper Sweeper.").send(commandSender);
            return true;
        }

        Player player = (Player) commandSender;

        new CreeperSweeperGameMenu(6, 8).openInventory(player);

        return true;
    }
}
