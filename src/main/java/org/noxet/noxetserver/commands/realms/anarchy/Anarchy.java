package org.noxet.noxetserver.commands.realms.anarchy;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.noxet.noxetserver.realm.RealmManager;
import org.noxet.noxetserver.messaging.ErrorMessage;

import static org.noxet.noxetserver.realm.RealmManager.migrateToRealm;

@SuppressWarnings("ALL")
public class Anarchy implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(!(commandSender instanceof Player)) {
            new ErrorMessage(ErrorMessage.ErrorType.COMMON, "Only players can be sent to the Anarchy server.").send(commandSender);
            return true;
        }

        migrateToRealm((Player) commandSender, RealmManager.Realm.ANARCHY);

        return true;
    }
}
