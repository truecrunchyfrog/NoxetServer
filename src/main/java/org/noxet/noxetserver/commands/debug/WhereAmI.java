package org.noxet.noxetserver.commands.debug;

import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.noxet.noxetserver.RealmManager;
import org.noxet.noxetserver.messaging.ErrorMessage;
import org.noxet.noxetserver.messaging.Message;

public class WhereAmI implements CommandExecutor {
    @Override
    @SuppressWarnings("NullableProblems")
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(!(commandSender instanceof Player)) {
            new ErrorMessage(ErrorMessage.ErrorType.COMMON, "Only players can check where they are.").send(commandSender);
            return true;
        }

        Player player = (Player) commandSender;

        World world = player.getWorld();
        RealmManager.Realm realm = RealmManager.getCurrentRealm(player);

        new Message("World ID: " + world.getName() + " @ Realm: " + (realm != null ? realm.getDisplayName() : "Not in a realm")).send(player);

        return true;
    }
}
