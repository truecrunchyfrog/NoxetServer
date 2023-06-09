package org.noxet.noxetserver.commands.misc;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.noxet.noxetserver.NoxetServer;
import org.noxet.noxetserver.UsernameStorageManager;
import org.noxet.noxetserver.messaging.NoxetErrorMessage;
import org.noxet.noxetserver.messaging.NoxetNoteMessage;
import org.noxet.noxetserver.messaging.NoxetSuccessMessage;
import org.noxet.noxetserver.playerdata.PlayerDataManager;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Block implements TabExecutor {
    @Override
    @SuppressWarnings("NullableProblems")
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(!(commandSender instanceof Player)) {
            new NoxetErrorMessage(NoxetErrorMessage.ErrorType.COMMON, "Only players can block players.").send(commandSender);
            return true;
        }

        Player player = (Player) commandSender;

        if(strings.length == 0) {
            new NoxetErrorMessage(NoxetErrorMessage.ErrorType.ARGUMENT, "Missing argument: player to block.").send(player);
            return true;
        }

        UUID uuidToBlock = new UsernameStorageManager().getUUIDFromUsernameOrUUID(strings[0]);

        if(uuidToBlock == null) {
            new NoxetErrorMessage(NoxetErrorMessage.ErrorType.COMMON, "That player is not registered.").send(player);
            return true;
        }

        String blockName = UsernameStorageManager.getCasedUsernameFromUUID(uuidToBlock); // This works anyway.

        PlayerDataManager playerDataManager = new PlayerDataManager(player);

        if(player.getUniqueId().equals(uuidToBlock)) {
            new NoxetErrorMessage(NoxetErrorMessage.ErrorType.COMMON, "You cannot block yourself.").send(player);
            return true;
        }

        if(playerDataManager.doesContain(PlayerDataManager.Attribute.BLOCKED_PLAYERS, uuidToBlock.toString())) {
            new NoxetErrorMessage(NoxetErrorMessage.ErrorType.COMMON, "You have already blocked " + blockName + ".").send(player);
            return true;
        }

        if(playerDataManager.getListSize(PlayerDataManager.Attribute.BLOCKED_PLAYERS) >= 500) {
            new NoxetErrorMessage(NoxetErrorMessage.ErrorType.COMMON, "You can only block up to 500 players.").send(player);
            return true;
        }

        playerDataManager.addToStringList(PlayerDataManager.Attribute.BLOCKED_PLAYERS, uuidToBlock.toString()).save();

        new NoxetSuccessMessage(blockName + " is now blocked. They player can no longer message, friend request or TPA you.").send(player);
        new NoxetNoteMessage("Any remaining friend or TPA request from/to them is kept.").send(player);

        return true;
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        if(!(commandSender instanceof Player))
            return null;

        if(strings.length == 1) {
            Player playerToRecommend = NoxetServer.getPlugin().getServer().getPlayer(strings[0]);
            if(playerToRecommend != null)
                return Collections.singletonList(playerToRecommend.getName());
        }

        return null;
    }
}
