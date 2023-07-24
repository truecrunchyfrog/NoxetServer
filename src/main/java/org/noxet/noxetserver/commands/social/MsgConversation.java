package org.noxet.noxetserver.commands.social;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.noxet.noxetserver.NoxetServer;
import org.noxet.noxetserver.util.UsernameStorageManager;
import org.noxet.noxetserver.messaging.*;
import org.noxet.noxetserver.playerdata.PlayerDataManager;
import org.noxet.noxetserver.util.TextBeautifier;

import java.util.*;

public class MsgConversation implements TabExecutor, Listener {
    public MsgConversation() {
        NoxetServer.getPlugin().getServer().getPluginManager().registerEvents(this, NoxetServer.getPlugin());
    }

    private static final Map<Player, Player> playerConversationChannels = new HashMap<>();

    @Override
    @SuppressWarnings("NullableProblems")
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(!(commandSender instanceof Player)) {
            new ErrorMessage(ErrorMessage.ErrorType.COMMON, "Only players can hold conversations with other players.").send(commandSender);
            return true;
        }

        Player player = (Player) commandSender;

        if(strings.length == 0) {
            new ErrorMessage(ErrorMessage.ErrorType.ARGUMENT, "Missing argument (player to message or toggle/block).").send(player);
            return true;
        }


        PlayerDataManager playerDataManager = new PlayerDataManager(player);

        if(strings[0].equalsIgnoreCase("toggle")) {
            boolean isMsgDisabled = (boolean) playerDataManager.get(PlayerDataManager.Attribute.MSG_DISABLED);

            playerDataManager.set(PlayerDataManager.Attribute.MSG_DISABLED, !isMsgDisabled).save();

            new SuccessMessage("Noxet direct messaging was " + (isMsgDisabled ? "enabled (you can now /msg players)" : "disabled (you can no longer /msg players)") + ".").send(player);

            return true;
        }

        if((boolean) playerDataManager.get(PlayerDataManager.Attribute.MSG_DISABLED)) {
            new ErrorMessage(ErrorMessage.ErrorType.COMMON, "You have disabled messaging.").send(player);
            return true;
        }

        UUID playerToMessageUUID = new UsernameStorageManager().getUUIDFromUsernameOrUUID(strings[0]);

        if(playerToMessageUUID == null) {
            new ErrorMessage(ErrorMessage.ErrorType.COMMON, "That player is not registered on Noxet.org.").send(player);
            return true;
        }

        Player playerToMessage = NoxetServer.getPlugin().getServer().getPlayer(playerToMessageUUID);

        if(playerToMessage == null) {
            new ErrorMessage(ErrorMessage.ErrorType.COMMON, "That player is not online.").send(player);
            return true;
        }

        if(player.equals(playerToMessage)) {
            new ErrorMessage(ErrorMessage.ErrorType.COMMON, "You cannot message yourself!").send(player);
            return true;
        }

        if(!mayPlayerChatWithPlayer(player, playerToMessage)) {
            new ErrorMessage(ErrorMessage.ErrorType.COMMON, "You cannot message this player.").send(player);
            return true;
        }

        String messageToSend = null;

        if(strings.length >= 2) {
            StringBuilder concatMessageArgs = new StringBuilder();
            for(int i = 1; i < strings.length; i++)
                concatMessageArgs.append(strings[i]);
            messageToSend = concatMessageArgs.toString();
        }

        PlayerDataManager targetPlayerDataManager = new PlayerDataManager(playerToMessage);

        //noinspection unchecked
        List<String> playerSpokenToList = (List<String>) playerDataManager.get(PlayerDataManager.Attribute.MSG_SPOKEN_TO);

        //noinspection unchecked
        if(!((List<String>) targetPlayerDataManager.get(PlayerDataManager.Attribute.MSG_SPOKEN_TO)).contains(player.getUniqueId().toString()) && !Friend.areFriends(player.getUniqueId(), playerToMessageUUID)) {
            // Target player does not have this player in their conversation list.

            if(playerSpokenToList.contains(playerToMessage.getUniqueId().toString())) {
                // This player has the other player in their conversation list.
                // This means that this player has already taken contact with the target player, and should not be
                // allowed to contact again until they receive a reply from target.

                new ErrorMessage(ErrorMessage.ErrorType.COMMON, "Please wait for them to reply to your first message before you can send another.").send(player);
                return true;
            }

            if(messageToSend == null) {
                new ErrorMessage(ErrorMessage.ErrorType.ARGUMENT, "You must initiate the new conversation with a message. Missing message.").send(player);
                return true;
            }

            // Create a new message request:

            playerDataManager.addToStringList(PlayerDataManager.Attribute.MSG_SPOKEN_TO, playerToMessageUUID.toString()).save();

            getConversationMessage(player, MessageDirectionType.INCOMING, messageToSend)
                    .addButton(
                            "Greet",
                            ChatColor.GREEN,
                            "Send a greeting reply and allow them to talk to you",
                            "msg " + player.getName() + " Greetings!"
                    )
                    .send(playerToMessage);

            new NoteMessage(player.getName() + " just messaged you for the first time.\n" +
                    "You need to reply first before they can send another message.\n" +
                    "If you don't want to talk to them, simply ignore their message and they cannot message you again.").send(playerToMessage);

            getConversationMessage(playerToMessage, MessageDirectionType.OUTGOING, messageToSend).send(player);

            new NoteMessage(playerToMessage.getName() + " needs to reply before you can continue the conversation with them.").send(player);
            return true;
        }

        if(!playerSpokenToList.contains(playerToMessage.getUniqueId().toString()) && !Friend.areFriends(player.getUniqueId(), playerToMessageUUID)) {
            // This counts as a reply. Target player has been accepted and conversation created.

            playerSpokenToList.add(playerToMessage.getUniqueId().toString());

            playerDataManager.set(PlayerDataManager.Attribute.MSG_SPOKEN_TO, playerSpokenToList).save();

            new SuccessMessage(player.getName() + " accepted the conversation with you. You can now message them.").send(playerToMessage);
            new SuccessMessage(playerToMessage.getName() + " can now message you.").send(player);
        }

        if(messageToSend != null) {
            // Send message:
            getConversationMessage(playerToMessage, MessageDirectionType.OUTGOING, messageToSend).send(player);
            getConversationMessage(player, MessageDirectionType.INCOMING, messageToSend).send(playerToMessage);
        } else {
            // Toggle conversation mode:
            if(playerConversationChannels.remove(player, playerToMessage)) {
                new Message("§cYou exited conversation mode with §f" + playerToMessage + "§c.").send(player);
                return true;
            }

            playerConversationChannels.put(player, playerToMessage);
            new Message("§3Entered conversation mode. Messages you send will be messaged privately to §d" + playerToMessage.getName() + "§3.")
                    .addButton(
                            "Exit",
                            ChatColor.RED,
                            "Exit conversation mode",
                            "msg " + playerToMessage.getName())
                    .send(player);
        }

        return true;
    }

    public static void clearActiveConversationModes(Player player) {
        for(Map.Entry<Player, Player> conversationEntry : playerConversationChannels.entrySet()) {
            if(player.equals(conversationEntry.getValue()))
                new Message("§eYou exit conversation mode because " + player.getName() + " left.").send(conversationEntry.getKey());
            else if(!player.equals(conversationEntry.getKey()))
                continue;

            playerConversationChannels.remove(conversationEntry.getKey());
        }
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        if(!(commandSender instanceof Player))
            return null;

        List<String> completions = new ArrayList<>();

        if(strings.length == 1) {
            if(strings[0].isEmpty())
                new Message("§5Enter a player to message, or an action from the shown menu.").send(commandSender);

            Player playerToRecommend = NoxetServer.getPlugin().getServer().getPlayer(strings[0]);

            if(playerToRecommend != null)
                completions.add(playerToRecommend.getName());

            completions.add("toggle");
        } else if(strings.length == 2) {
            //noinspection SwitchStatementWithTooFewBranches
            switch(strings[0].toLowerCase()) {
                case "toggle":
                    break;
                default:
                    if(strings[1].isEmpty())
                        new Message("§5Enter a message to send to §f" + strings[0] + "§5:").send(commandSender);
            }
        }

        return completions;
    }

    private static boolean mayPlayerChatWithPlayer(Player askingPlayer, Player targetPlayer) {
        PlayerDataManager askingPDM = new PlayerDataManager(askingPlayer),
                targetPDM = new PlayerDataManager(targetPlayer);
        return !(boolean) targetPDM.get(PlayerDataManager.Attribute.MSG_DISABLED) &&
                !targetPDM.doesContain(PlayerDataManager.Attribute.BLOCKED_PLAYERS, askingPlayer.getUniqueId().toString()) &&
                !askingPDM.doesContain(PlayerDataManager.Attribute.BLOCKED_PLAYERS, targetPlayer.getUniqueId().toString());
    }

    private enum MessageDirectionType {
        INCOMING, OUTGOING
    }

    private static Message getConversationMessage(Player oppositePlayer, MessageDirectionType direction, String message) {
        return new Message(null).add(
                (direction.equals(MessageDirectionType.OUTGOING) ? ("§3→✉ " + TextBeautifier.beautify("to") + "  ") : "§7✉→ " + TextBeautifier.beautify("from")) + " §d" + oppositePlayer.getName() + "§5◇ §f" + message,
                (direction.equals(MessageDirectionType.OUTGOING) ? "You sent this" : oppositePlayer.getName() + " sent this (use /msg " + oppositePlayer.getName() + " <message> to reply)")
        );
    }

    @EventHandler
    public void onAsyncPlayerChat(AsyncPlayerChatEvent e) {
        if(playerConversationChannels.containsKey(e.getPlayer())) {
            e.getPlayer().performCommand("msg " + playerConversationChannels.get(e.getPlayer()).getName() + " " + e.getMessage());
            e.setCancelled(true);
        }
    }
}
