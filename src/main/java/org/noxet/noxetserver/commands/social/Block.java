package org.noxet.noxetserver.commands.social;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.noxet.noxetserver.NoxetServer;
import org.noxet.noxetserver.UsernameStorageManager;
import org.noxet.noxetserver.messaging.ErrorMessage;
import org.noxet.noxetserver.messaging.NoteMessage;
import org.noxet.noxetserver.messaging.SuccessMessage;
import org.noxet.noxetserver.playerdata.PlayerDataManager;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Block implements TabExecutor {
    @Override
    @SuppressWarnings("NullableProblems")
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(!(commandSender instanceof Player)) {
            new ErrorMessage(ErrorMessage.ErrorType.COMMON, "Only players can block players.").send(commandSender);
            return true;
        }

        Player player = (Player) commandSender;

        if(strings.length == 0) {
            new ErrorMessage(ErrorMessage.ErrorType.ARGUMENT, "Missing argument: player to block.").send(player);
            return true;
        }

        UUID uuidToBlock = new UsernameStorageManager().getUUIDFromUsernameOrUUID(strings[0]);

        if(uuidToBlock == null) {
            new ErrorMessage(ErrorMessage.ErrorType.COMMON, "That player is not registered.").send(player);
            return true;
        }

        String blockName = UsernameStorageManager.getCasedUsernameFromUUID(uuidToBlock); // This works anyway.

        PlayerDataManager playerDataManager = new PlayerDataManager(player);

        if(player.getUniqueId().equals(uuidToBlock)) {
            new ErrorMessage(ErrorMessage.ErrorType.COMMON, "You cannot block yourself.").send(player);
            return true;
        }

        if(playerDataManager.doesContain(PlayerDataManager.Attribute.BLOCKED_PLAYERS, uuidToBlock.toString())) {
            new ErrorMessage(ErrorMessage.ErrorType.COMMON, "You have already blocked " + blockName + ".").send(player);
            return true;
        }

        if(Friend.areFriends(player.getUniqueId(), uuidToBlock)) {
            new ErrorMessage(ErrorMessage.ErrorType.COMMON, "You are friends with this player. Please unfriend them before you can block them.").send(player);
            return true;
        }

        if(Friend.hasReceivedFriendRequestFrom(player.getUniqueId(), uuidToBlock)) {
            Friend.denyRequest(player.getUniqueId(), uuidToBlock);
            new NoteMessage("Friend request from " + blockName + " was automatically denied due to block.").send(player);
        }

        if(Friend.hasReceivedFriendRequestFrom(uuidToBlock, player.getUniqueId())) {
            Friend.denyRequest(uuidToBlock, player.getUniqueId());
            new NoteMessage("Friend request to " + blockName + " was canceled due to block.").send(player);
        }

        if(playerDataManager.getListSize(PlayerDataManager.Attribute.BLOCKED_PLAYERS) >= 500) {
            new ErrorMessage(ErrorMessage.ErrorType.COMMON, "You can only block up to 500 players.").send(player);
            return true;
        }

        playerDataManager.addToStringList(PlayerDataManager.Attribute.BLOCKED_PLAYERS, uuidToBlock.toString()).save();

        new SuccessMessage(blockName + " is now blocked. They player can no longer message, friend request or TPA you.").send(player);

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
