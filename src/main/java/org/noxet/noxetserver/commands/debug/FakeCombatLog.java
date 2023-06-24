package org.noxet.noxetserver.commands.debug;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.noxet.noxetserver.CombatLogging;
import org.noxet.noxetserver.messaging.ErrorMessage;

public class FakeCombatLog implements CommandExecutor {
    @Override
    @SuppressWarnings("NullableProblems")
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(!commandSender.isOp()) {
            new ErrorMessage(ErrorMessage.ErrorType.PERMISSION, "Only operators can fake combat logging.").send(commandSender);
            return true;
        }

        if(!(commandSender instanceof Player)) {
            new ErrorMessage(ErrorMessage.ErrorType.COMMON, "Only players can fake combat logging.").send(commandSender);
            return true;
        }

        Player player = (Player) commandSender;

        CombatLogging.triggerCombatLog(player);

        return true;
    }
}
