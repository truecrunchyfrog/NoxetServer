package org.noxet.noxetserver.commands.misc;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.noxet.noxetserver.RealmManager;
import org.noxet.noxetserver.messaging.ErrorMessage;

import static org.noxet.noxetserver.RealmManager.migrateToRealm;

public class CanvasWorld implements CommandExecutor {
    @Override
    @SuppressWarnings("NullableProblems")
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(!commandSender.isOp()) {
            new ErrorMessage(ErrorMessage.ErrorType.PERMISSION, "Only operators can do this.").send(commandSender);
            return true;
        }

        if(!(commandSender instanceof Player)) {
            new ErrorMessage(ErrorMessage.ErrorType.COMMON, "Only players can do this.").send(commandSender);
            return true;
        }

        migrateToRealm((Player) commandSender, RealmManager.Realm.CANVAS);

        return true;
    }
}
