package org.noxet.noxetserver.commands.misc;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.noxet.noxetserver.CombatLogging;
import org.noxet.noxetserver.messaging.NoxetErrorMessage;

public class FakeCombatLog implements CommandExecutor {
    @Override
    @SuppressWarnings("NullableProblems")
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(!commandSender.isOp()) {
            new NoxetErrorMessage(NoxetErrorMessage.ErrorType.PERMISSION, "Only operators can fake combat logging.").send(commandSender);
            return true;
        }

        if(!(commandSender instanceof Player)) {
            new NoxetErrorMessage(NoxetErrorMessage.ErrorType.COMMON, "Only players can fake combat logging.").send(commandSender);
            return true;
        }

        Player player = (Player) commandSender;

        CombatLogging.triggerCombatLog(player);

        return true;
    }
}
