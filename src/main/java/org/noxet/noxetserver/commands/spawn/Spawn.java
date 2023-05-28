package org.noxet.noxetserver.commands.spawn;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.noxet.noxetserver.RealmManager;
import org.noxet.noxetserver.messaging.NoxetErrorMessage;

import static org.noxet.noxetserver.RealmManager.getCurrentRealm;
import static org.noxet.noxetserver.RealmManager.goToSpawn;

@SuppressWarnings("ALL")
public class Spawn implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(!(commandSender instanceof Player)) {
            new NoxetErrorMessage("Only players can be sent to the spawn.").send(commandSender);
            return true;
        }

        Player player = (Player) commandSender;
        RealmManager.Realm realm = getCurrentRealm(player);
        if(realm == null || realm.doesAllowSpawnCommand()) {
            goToSpawn(player);
        } else {
            new NoxetErrorMessage("You cannot use this command here.").send(player);
        }

        return true;
    }
}
