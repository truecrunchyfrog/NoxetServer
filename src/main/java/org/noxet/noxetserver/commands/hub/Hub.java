package org.noxet.noxetserver.commands.hub;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.noxet.noxetserver.messaging.NoxetErrorMessage;

import static org.noxet.noxetserver.RealmManager.goToHub;

public class Hub implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(!(commandSender instanceof Player)) {
            new NoxetErrorMessage("Only players can be sent to the hub.").send(commandSender);
            return true;
        }

        Player player = (Player) commandSender;
        goToHub(player);

        return true;
    }
}
