package org.noxet.noxetserver.commands.misc;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.noxet.noxetserver.RealmDataManager;
import org.noxet.noxetserver.messaging.NoxetErrorMessage;
import org.noxet.noxetserver.messaging.NoxetSuccessMessage;

import static org.noxet.noxetserver.RealmManager.getCurrentRealm;

public class ResetRealmSpawn implements CommandExecutor {
    @Override
    @SuppressWarnings("NullableProblems")
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(!(commandSender instanceof Player)) {
            new NoxetErrorMessage(NoxetErrorMessage.ErrorType.COMMON, "Only players can reset realm spawns.").send(commandSender);
            return true;
        }

        Player player = (Player) commandSender;

        if(!player.isOp()) {
            new NoxetErrorMessage(NoxetErrorMessage.ErrorType.PERMISSION, "Only operators can reset realm spawns.").send(commandSender);
            return true;
        }

        new RealmDataManager().setSoftSpawnLocation(getCurrentRealm(player), null);
        new NoxetSuccessMessage("Erased spawn for realm.").send(player);

        return true;
    }
}
