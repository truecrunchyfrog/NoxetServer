package org.noxet.noxetserver.commands.misc;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryView;
import org.noxet.noxetserver.NoxetServer;
import org.noxet.noxetserver.RealmManager;
import org.noxet.noxetserver.messaging.ErrorMessage;

public class EnderChest implements CommandExecutor {
    @Override
    @SuppressWarnings("NullableProblems")
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(!(commandSender instanceof Player)) {
            new ErrorMessage(ErrorMessage.ErrorType.COMMON, "Only players can view their ender chest.").send(commandSender);
            return true;
        }

        Player player = (Player) commandSender;

        RealmManager.Realm realm = RealmManager.getCurrentRealm(player);

        if(realm == null || !realm.doesAllowTeleportationMethods()) {
            new ErrorMessage(ErrorMessage.ErrorType.PERMISSION, "You may not use /enderchest here.").send(player);
            return true;
        }

        Player peekOn = null;

        if(strings.length > 0)
            if(player.isOp()) {
                peekOn = NoxetServer.getPlugin().getServer().getPlayer(strings[0]);
            } else {
                new ErrorMessage(ErrorMessage.ErrorType.PERMISSION, "You are not allowed to open other players' ender chests.").send(player);
                return true;
            }

        InventoryView inventoryView = player.openInventory((peekOn == null ? player : peekOn).getEnderChest());

        if(inventoryView == null) {
            new ErrorMessage(ErrorMessage.ErrorType.COMMON, "Failed to open inventory.").send(player);
            return true;
        }

        if(peekOn != null)
            inventoryView.setTitle(peekOn.getName() + "'s " + inventoryView.getTitle());

        return true;
    }
}
