package org.noxet.noxetserver.commands.misc;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.help.HelpMap;
import org.bukkit.help.HelpTopic;
import org.noxet.noxetserver.NoxetServer;
import org.noxet.noxetserver.messaging.NoxetErrorMessage;
import org.noxet.noxetserver.messaging.NoxetMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class DoAs implements TabExecutor {
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(!commandSender.isOp()) {
            new NoxetErrorMessage("You must be an operator to impersonate players.").send(commandSender);
            return true;
        }

        if(strings.length == 0) {
            new NoxetErrorMessage("Missing argument: player to run command as.").send(commandSender);
            return true;
        }

        Player doAsPlayer = NoxetServer.getPlugin().getServer().getPlayer(strings[0]);

        if(doAsPlayer == null) {
            new NoxetErrorMessage("That is not a valid player.").send(commandSender);
            return true;
        }

        List<String> commandArgs = new ArrayList<>(Arrays.asList(strings));
        commandArgs.remove(0); // Remove the player name argument.

        Command commandToRun = NoxetServer.getPlugin().getServer().getPluginCommand(commandArgs.get(0));

        if(command.equals(commandToRun)) {
            new NoxetErrorMessage("Recursive doas not allowed!").send(commandSender);
            return true;
        }

        StringBuilder commandWithArgs = new StringBuilder();

        int i = 0;
        for(String arg : commandArgs)
            commandWithArgs.append(i++ != 0 ? " " : "").append(arg);

        new NoxetMessage("Performing command as \"" + doAsPlayer.getName() + "\": §o/" + commandWithArgs).send(commandSender);
        doAsPlayer.performCommand(commandWithArgs.toString());
        new NoxetMessage("§aCommand finished.").send(commandSender);

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        if(!commandSender.isOp())
            return null;

        List<String> completions = new ArrayList<>();

        if(strings.length == 1) {
            Player player = NoxetServer.getPlugin().getServer().getPlayer(strings[0]);
            if(player == null)
                return null;
            completions.add(player.getName());
        } else if(strings.length == 2) {
            Player player = NoxetServer.getPlugin().getServer().getPlayer(strings[0]);
            if(player == null)
                return null;
            HelpMap commandMap = NoxetServer.getPlugin().getServer().getHelpMap();
            Collection<HelpTopic> commands = commandMap.getHelpTopics();

            for(HelpTopic eachCommand : commands)
                try{
                    if(eachCommand.getName().startsWith("/") && eachCommand.canSee(player))
                        completions.add(eachCommand.getName().substring(1));
                } catch(IllegalStateException ignored) {}
        } else if(strings.length > 2) {
            Player player = NoxetServer.getPlugin().getServer().getPlayer(strings[0]);
            if(player == null)
                return null;
            Command doAsCommand = NoxetServer.getPlugin().getServer().getPluginCommand(strings[1]);
            if(doAsCommand == null || !doAsCommand.testPermissionSilent(player) || doAsCommand.equals(command))
                return null;

            List<String> commandArgs = Arrays.asList(strings);
            commandArgs.remove(0); // Player name
            commandArgs.remove(1); // Command name

            return doAsCommand.tabComplete(player, doAsCommand.getName(), commandArgs.toArray(new String[0]), player.getLocation());
        }

        return completions;
    }
}
