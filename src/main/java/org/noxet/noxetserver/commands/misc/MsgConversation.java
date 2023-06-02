package org.noxet.noxetserver.commands.misc;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.noxet.noxetserver.NoxetServer;
import org.noxet.noxetserver.messaging.NoxetErrorMessage;
import org.noxet.noxetserver.messaging.NoxetMessage;
import org.noxet.noxetserver.messaging.NoxetNoteMessage;
import org.noxet.noxetserver.playerdata.PlayerDataManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MsgConversation implements TabExecutor {
    private static final Map<Player, Player> playerConversations = new HashMap<>();
    private static final Map<Player, Player> pendingConversationConfirmations = new HashMap<>(); // Key: player asking to message. Value: player to accept message.

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(!(commandSender instanceof Player)) {
            new NoxetErrorMessage(NoxetErrorMessage.ErrorType.COMMON, "Only players can hold conversations with other players.").send(commandSender);
            return true;
        }

        Player player = (Player) commandSender;

        if(strings.length == 0) {
            new NoxetErrorMessage(NoxetErrorMessage.ErrorType.ARGUMENT, "Missing player to message.").send(player);
            return true;
        }

        Player playerToMessage = NoxetServer.getPlugin().getServer().getPlayer(strings[0]);

        if(playerToMessage == null) {
            new NoxetErrorMessage(NoxetErrorMessage.ErrorType.ARGUMENT, "Invalid player.").send(player);
            return true;
        }

        if(player.equals(playerToMessage)) {
            new NoxetErrorMessage(NoxetErrorMessage.ErrorType.ARGUMENT, "You cannot message yourself!").send(player);
            return true;
        }

        if(!mayPlayerChatWithPlayer(player, playerToMessage)) {
            new NoxetErrorMessage(NoxetErrorMessage.ErrorType.COMMON, "You cannot message this player.").send(player);
            return true;
        }

        PlayerDataManager targetPlayerDataManager = new PlayerDataManager(playerToMessage);

        String messageToSend;

        if(strings.length >= 2) {
            StringBuilder concatMessageArgs = new StringBuilder();
            for(int i = 1; i < strings.length; i++)
                concatMessageArgs.append(strings[i]);
            messageToSend = concatMessageArgs.toString();
        }

        //noinspection unchecked
        if(((List<String>) targetPlayerDataManager.get(PlayerDataManager.Attribute.MSG_TALKED_WITH)).contains(player.getUniqueId().toString())) {
            new NoxetNoteMessage("You have not spoken to this player before. Therefore you can only send one message before they accept your conversation request.").send(player);

            if(pendingConversationConfirmations.containsKey(player)) {
                new NoxetErrorMessage(NoxetErrorMessage.ErrorType.COMMON, "You have already sent an unconfirmed message. Please wait for " + pendingConversationConfirmations.get(player).getName() + " to accept your message - or cancel it - before you can open a new conversation request.").send(player);
                return true;
            }

            if(messageToSend == null) {
                new NoxetErrorMessage(NoxetErrorMessage.ErrorType.ARGUMENT, "You must initiate the new conversation with a message. Missing message.").send(player);
                return true;
            }

            // Create a new message request:

            pendingConversationConfirmations.put(player, playerToMessage);

            new NoxetMessage("§3" + player.getDisplayName() + "§e has sent you a message: §7" + messageToSend + "\n")
                    .addButton(
                            "Accept conversation"
                    )
                    .send(playerToMessage);

            // TODO fix conversation invitation buttons (allow, deny); timeout and also delete pending when either leave
        }

        // todo more logic here

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        return null;
    }

    @SuppressWarnings("unchecked")
    private static boolean mayPlayerChatWithPlayer(Player askingPlayer, Player targetPlayer) {
        PlayerDataManager playerDataManager = new PlayerDataManager(targetPlayer);
        return !(boolean) playerDataManager.get(PlayerDataManager.Attribute.MSG_DISABLED) &&
                !((List<String>) playerDataManager.get(PlayerDataManager.Attribute.MSG_BLOCKED_PLAYERS)).contains(askingPlayer.getUniqueId().toString());
    }
}
