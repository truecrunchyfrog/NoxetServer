package org.noxet.noxetserver.commands.teleportation;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.noxet.noxetserver.realm.RealmManager;
import org.noxet.noxetserver.messaging.ErrorMessage;

import static org.noxet.noxetserver.realm.RealmManager.getCurrentRealm;
import static org.noxet.noxetserver.realm.RealmManager.goToSpawn;

@SuppressWarnings("ALL")
public class Spawn implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(!(commandSender instanceof Player)) {
            new ErrorMessage(ErrorMessage.ErrorType.COMMON, "Only players can be sent to the spawn.").send(commandSender);
            return true;
        }

        Player player = (Player) commandSender;
        RealmManager.Realm realm = getCurrentRealm(player);
        if(realm == null || realm.doesAllowTeleportationMethods()) {
            goToSpawn(player);
        } else {
            new ErrorMessage(ErrorMessage.ErrorType.COMMON, "You cannot use /spawn in this realm.").send(player);
        }

        return true;
    }
}
