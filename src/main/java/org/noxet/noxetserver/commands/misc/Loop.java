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

public class Loop implements TabExecutor {
    @Override
    @SuppressWarnings("NullableProblems")
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(!(commandSender instanceof Player)) {
            new NoxetErrorMessage(NoxetErrorMessage.ErrorType.COMMON, "Only players can loop commands.").send(commandSender);
            return true;
        }

        Player player = (Player) commandSender;

        if(!player.isOp()) {
            new NoxetErrorMessage(NoxetErrorMessage.ErrorType.PERMISSION, "Only operators can loop commands.").send(player);
            return true;
        }

        if(strings.length == 0) {
            new NoxetErrorMessage(NoxetErrorMessage.ErrorType.ARGUMENT, "Missing argument: times to repeat.").send(player);
            return true;
        }

        int amount;

        try {
            amount = Integer.parseInt(strings[0]);
        } catch(NumberFormatException e) {
            new NoxetErrorMessage(NoxetErrorMessage.ErrorType.ARGUMENT, "Times to repeat must be an integer.").send(player);
            return true;
        }

        if(amount < 2 || amount > 500) {
            new NoxetErrorMessage(NoxetErrorMessage.ErrorType.ARGUMENT, "Amount must be within 2-500 (inclusive).").send(player);
            return true;
        }

        if(strings.length < 2 || strings[1].isEmpty()) {
            new NoxetErrorMessage(NoxetErrorMessage.ErrorType.ARGUMENT, "Missing command to loop.").send(player);
            return true;
        }

        if(command.getName().equalsIgnoreCase(strings[1]) || command.getAliases().contains(strings[1].toLowerCase())) {
            new NoxetErrorMessage(NoxetErrorMessage.ErrorType.ARGUMENT, "Nested loops are not allowed.").send(player);
            return true;
        }

        StringBuilder commandToRunSB = new StringBuilder();

        for(int i = 1; i < strings.length; i++)
            commandToRunSB.append(i != 1 ? " " : "").append(strings[i]);

        String commandToRunStr = commandToRunSB.toString();

        new NoxetMessage("§3Looping command §b" + amount + "§3 times: §b/" + commandToRunStr).send(player);

        for(int i = 0; i < amount; i++)
            player.performCommand(commandToRunStr);

        new NoxetMessage("§3Loop finished.").send(player);

        return true;
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        if(!(commandSender instanceof Player))
            return null;

        List<String> completions = new ArrayList<>();

        if(strings.length == 1) {
            completions.addAll(Arrays.asList("5", "10", "20", "50", "100", "150", "300", "500"));
        } else if(strings.length == 2) {
            HelpMap commandMap = NoxetServer.getPlugin().getServer().getHelpMap();
            Collection<HelpTopic> commands = commandMap.getHelpTopics();

            for(HelpTopic eachCommand : commands)
                try{
                    if(eachCommand.getName().startsWith("/") && eachCommand.canSee(commandSender))
                        completions.add(eachCommand.getName().substring(1));
                } catch(IllegalStateException ignored) {}
        } else if(strings.length > 2) {
            Command commandToCheck = NoxetServer.getPlugin().getServer().getPluginCommand(strings[1]);
            if(commandToCheck == null || !commandToCheck.testPermissionSilent(commandSender) || commandToCheck.equals(command))
                return null;

            List<String> commandArgs = Arrays.asList(strings);
            commandArgs.remove(0); // Amount
            commandArgs.remove(1); // Command name

            return commandToCheck.tabComplete(commandSender, commandToCheck.getName(), commandArgs.toArray(new String[0]), ((Player) commandSender).getLocation());
        }

        return completions;
    }
}
