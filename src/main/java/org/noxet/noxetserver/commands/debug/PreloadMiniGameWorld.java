package org.noxet.noxetserver.commands.debug;

import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.noxet.noxetserver.NoxetServer;
import org.noxet.noxetserver.messaging.ErrorMessage;
import org.noxet.noxetserver.messaging.SuccessMessage;
import org.noxet.noxetserver.minigames.MiniGameController;
import org.noxet.noxetserver.util.FancyTimeConverter;

import java.util.ArrayList;
import java.util.List;

public class PreloadMiniGameWorld implements CommandExecutor {
    @Override
    @SuppressWarnings("NullableProblems")
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(!commandSender.isOp()) {
            new ErrorMessage(ErrorMessage.ErrorType.PERMISSION, "Only operators can preload the mini-game world.").send(commandSender);
            return true;
        }

        List<World> alreadyLoaded = new ArrayList<>(NoxetServer.getPlugin().getServer().getWorlds());

        long initTimestamp = System.currentTimeMillis();

        World miniGameWorld = MiniGameController.getMiniGameWorld(); // Calling this method will load the world (and also create it if not existing).

        if(!alreadyLoaded.contains(miniGameWorld))
            new SuccessMessage("Successfully loaded mini-game world in " + FancyTimeConverter.deltaSecondsToFancyTime((int) (System.currentTimeMillis() - initTimestamp) / 1000) + ".").send(commandSender);
        else
            new ErrorMessage(ErrorMessage.ErrorType.COMMON, "Mini-game world is already loaded.").send(commandSender);

        return true;
    }
}
