package org.noxet.noxetserver.commands.admin.realm;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.noxet.noxetserver.realm.RealmDataManager;
import org.noxet.noxetserver.messaging.ErrorMessage;
import org.noxet.noxetserver.messaging.SuccessMessage;

import static org.noxet.noxetserver.realm.RealmManager.getCurrentRealm;

public class ResetRealmSpawn implements CommandExecutor {
    @Override
    @SuppressWarnings("NullableProblems")
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(!(commandSender instanceof Player)) {
            new ErrorMessage(ErrorMessage.ErrorType.COMMON, "Only players can reset realm spawns.").send(commandSender);
            return true;
        }

        Player player = (Player) commandSender;

        if(!player.isOp()) {
            new ErrorMessage(ErrorMessage.ErrorType.PERMISSION, "Only operators can reset realm spawns.").send(commandSender);
            return true;
        }

        new RealmDataManager().setSoftSpawnLocation(getCurrentRealm(player), null);
        new SuccessMessage("Erased spawn for realm.").send(player);

        return true;
    }
}
