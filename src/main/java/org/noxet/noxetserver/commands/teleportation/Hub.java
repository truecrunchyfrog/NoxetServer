package org.noxet.noxetserver.commands.teleportation;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.noxet.noxetserver.messaging.ErrorMessage;

import static org.noxet.noxetserver.realm.RealmManager.goToHub;

@SuppressWarnings("ALL")
public class Hub implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(!(commandSender instanceof Player)) {
            new ErrorMessage(ErrorMessage.ErrorType.COMMON, "Only players can be sent to the hub.").send(commandSender);
            return true;
        }

        goToHub((Player) commandSender);

        return true;
    }
}
