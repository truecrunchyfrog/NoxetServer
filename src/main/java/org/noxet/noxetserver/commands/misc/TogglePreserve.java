package org.noxet.noxetserver.commands.misc;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.noxet.noxetserver.NoxetServer;
import org.noxet.noxetserver.messaging.NoxetErrorMessage;
import org.noxet.noxetserver.messaging.NoxetSuccessMessage;

public class TogglePreserve implements CommandExecutor {
    @Override
    @SuppressWarnings("NullableProblems")
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(!commandSender.isOp()) {
            new NoxetErrorMessage(NoxetErrorMessage.ErrorType.PERMISSION, "Only operators can toggle preservation mode.").send(commandSender);
            return true;
        }

        NoxetServer.shouldAllowWorldPreservation = !NoxetServer.shouldAllowWorldPreservation;
        new NoxetSuccessMessage((NoxetServer.shouldAllowWorldPreservation ? "Enabled" : "Disabled") + " world preservation.").send(commandSender);

        return true;
    }
}
