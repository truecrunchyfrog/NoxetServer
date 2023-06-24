package org.noxet.noxetserver.commands.realms.smp;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.noxet.noxetserver.RealmManager;
import org.noxet.noxetserver.messaging.ErrorMessage;

import static org.noxet.noxetserver.RealmManager.migrateToRealm;

@SuppressWarnings("ALL")
public class SMP implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(!(commandSender instanceof Player)) {
            new ErrorMessage(ErrorMessage.ErrorType.COMMON, "Only players can be sent to the SMP server.").send(commandSender);
            return true;
        }

        migrateToRealm((Player) commandSender, RealmManager.Realm.SMP);

        return true;
    }
}
