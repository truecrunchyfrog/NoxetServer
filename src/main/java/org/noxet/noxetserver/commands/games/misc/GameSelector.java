package org.noxet.noxetserver.commands.games.misc;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.noxet.noxetserver.menus.inventory.GameNavigationMenu;
import org.noxet.noxetserver.messaging.ErrorMessage;

public class GameSelector implements CommandExecutor {
    @Override
    @SuppressWarnings("NullableProblems")
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(!(commandSender instanceof Player)) {
            new ErrorMessage(ErrorMessage.ErrorType.COMMON, "Only players can open the game selector menu.").send(commandSender);
            return true;
        }

        new GameNavigationMenu().openInventory((Player) commandSender);

        return true;
    }
}
