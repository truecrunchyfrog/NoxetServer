package org.noxet.noxetserver.commands.misc;

import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.noxet.noxetserver.RealmManager;
import org.noxet.noxetserver.messaging.NoxetErrorMessage;
import org.noxet.noxetserver.messaging.NoxetMessage;

public class WhereAmI implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(!(commandSender instanceof Player)) {
            new NoxetErrorMessage("Only players can check where they are.").send(commandSender);
            return true;
        }

        Player player = (Player) commandSender;

        World world = player.getWorld();
        RealmManager.Realm realm = RealmManager.getCurrentRealm(player);

        new NoxetMessage("World ID: " + world.getName() + " @ Realm: " + (realm != null ? realm.getDisplayName() : "Not in a realm")).send(player);

        return true;
    }
}
