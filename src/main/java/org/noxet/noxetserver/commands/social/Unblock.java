package org.noxet.noxetserver.commands.social;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.noxet.noxetserver.UsernameStorageManager;
import org.noxet.noxetserver.messaging.ErrorMessage;
import org.noxet.noxetserver.messaging.SuccessMessage;
import org.noxet.noxetserver.playerdata.PlayerDataManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Unblock implements TabExecutor {
    @Override
    @SuppressWarnings("NullableProblems")
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(!(commandSender instanceof Player)) {
            new ErrorMessage(ErrorMessage.ErrorType.COMMON, "Only players can unblock players.").send(commandSender);
            return true;
        }

        Player player = (Player) commandSender;

        if(strings.length == 0) {
            new ErrorMessage(ErrorMessage.ErrorType.ARGUMENT, "Missing argument: player to unblock.").send(player);
            return true;
        }

        UUID uuidToUnblock = new UsernameStorageManager().getUUIDFromUsernameOrUUID(strings[0]);

        if(uuidToUnblock == null) {
            new ErrorMessage(ErrorMessage.ErrorType.COMMON, "That player is not registered.").send(player);
            return true;
        }

        String unblockName = UsernameStorageManager.getCasedUsernameFromUUID(uuidToUnblock); // This works anyway.

        PlayerDataManager playerDataManager = new PlayerDataManager(player);

        if(!playerDataManager.doesContain(PlayerDataManager.Attribute.BLOCKED_PLAYERS, uuidToUnblock.toString())) {
            new ErrorMessage(ErrorMessage.ErrorType.COMMON, "You have not blocked " + unblockName + ".").send(player);
            return true;
        }

        playerDataManager.removeFromStringList(PlayerDataManager.Attribute.BLOCKED_PLAYERS, uuidToUnblock.toString()).save();

        new SuccessMessage(unblockName + " is no longer blocked. They player can now message, friend request and TPA you.").send(player);

        return true;
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        if(!(commandSender instanceof Player))
            return null;

        Player player = (Player) commandSender;

        List<String> completions = new ArrayList<>();

        if(strings.length == 1) {
            //noinspection unchecked
            for(String blockedUUIDString : (List<String>) new PlayerDataManager(player.getUniqueId()).get(PlayerDataManager.Attribute.BLOCKED_PLAYERS)) {
                UUID blockedUUID = new UsernameStorageManager().getUUIDFromUsernameOrUUID(blockedUUIDString);
                completions.add(blockedUUID != null ? UsernameStorageManager.getCasedUsernameFromUUID(blockedUUID) : blockedUUIDString);
            }
        }

        return completions;
    }
}
