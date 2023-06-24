package org.noxet.noxetserver.commands.games.creepersweeper;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.noxet.noxetserver.menus.inventory.ConfirmationMenu;
import org.noxet.noxetserver.messaging.ErrorMessage;
import org.noxet.noxetserver.messaging.NoteMessage;
import org.noxet.noxetserver.messaging.SuccessMessage;
import org.noxet.noxetserver.playerdata.PlayerDataManager;

public class ClearCreeperSweeperStats implements CommandExecutor {
    @Override
    @SuppressWarnings("NullableProblems")
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(!(commandSender instanceof Player)) {
            new ErrorMessage(ErrorMessage.ErrorType.COMMON, "Only players may clear their Creeper Sweeper stats.").send(commandSender);
            return true;
        }

        Player player = (Player) commandSender;

        new ConfirmationMenu("Clear all Creeper Sweeper stats?", () -> {
            PlayerDataManager playerDataManager = new PlayerDataManager(player);

            playerDataManager.remove(PlayerDataManager.Attribute.CREEPER_SWEEPER_WINS);
            playerDataManager.remove(PlayerDataManager.Attribute.CREEPER_SWEEPER_LOSSES);
            playerDataManager.remove(PlayerDataManager.Attribute.CREEPER_SWEEPER_TOTAL_WIN_PLAYTIME);

            playerDataManager.save();

            new SuccessMessage("Your Creeper Sweeper stats have been deleted.").send(player);
        }, () -> new NoteMessage("Phew! Your Creeper Sweeper game stats remain.").send(player)).openInventory(player);

        return true;
    }
}
