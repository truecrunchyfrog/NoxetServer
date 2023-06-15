package org.noxet.noxetserver.commands.misc;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.noxet.noxetserver.NoxetServer;
import org.noxet.noxetserver.UsernameStorageManager;
import org.noxet.noxetserver.menus.inventory.FriendsMenu;
import org.noxet.noxetserver.messaging.NoxetErrorMessage;
import org.noxet.noxetserver.messaging.NoxetMessage;
import org.noxet.noxetserver.messaging.NoxetNoteMessage;
import org.noxet.noxetserver.messaging.NoxetSuccessMessage;
import org.noxet.noxetserver.playerdata.PlayerDataManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class Friend implements TabExecutor {
    private static final int maxOutgoingRequests = 5, maxIncomingRequests = 20, maxFriends = 30;

    @Override
    @SuppressWarnings("NullableProblems")
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(!(commandSender instanceof Player)) {
            new NoxetErrorMessage(NoxetErrorMessage.ErrorType.COMMON, "Only players can have friends.").send(commandSender);
            return true;
        }

        Player player = (Player) commandSender;

        if(strings.length == 0) {
            new FriendsMenu(player).openInventory(player);
            return true;
        }

        if(strings[0].equalsIgnoreCase("add")) {
            if(strings.length < 2) {
                new NoxetErrorMessage(NoxetErrorMessage.ErrorType.ARGUMENT, "Missing argument: player to befriend.").send(player);
                return true;
            }

            UUID uuidToBefriend = new UsernameStorageManager().getUUIDFromUsernameOrUUID(strings[1]);

            if(uuidToBefriend == null) {
                new NoxetErrorMessage(NoxetErrorMessage.ErrorType.COMMON, "That player is not registered.").send(player);
                return true;
            }

            Player playerToBefriend = NoxetServer.getPlugin().getServer().getPlayer(uuidToBefriend); // Only works if the player is online.
            String befriendName = UsernameStorageManager.getCasedUsernameFromUUID(uuidToBefriend); // This works anyway.

            if(player.getUniqueId().equals(uuidToBefriend)) {
                new NoxetErrorMessage(NoxetErrorMessage.ErrorType.COMMON, "You cannot befriend yourself.").send(player);
                return true;
            }

            if(areFriends(player.getUniqueId(), uuidToBefriend)) {
                new NoxetErrorMessage(NoxetErrorMessage.ErrorType.COMMON, "You are already friends!").send(player);
                return true;
            }

            if(strings.length > 2 && strings[2].equals("!") && player.isOp()) {
                acceptRequest(uuidToBefriend, player.getUniqueId());
                new NoxetSuccessMessage("Forced friendships are the best! You are now friends with " + befriendName + ".").send(player);
                return true;
            }

            if(hasReceivedFriendRequestFrom(uuidToBefriend, player.getUniqueId())) {
                new NoxetErrorMessage(NoxetErrorMessage.ErrorType.COMMON, "You have already sent a friend request to them. Please wait for them to respond.").send(player);
                return true;
            }

            if(getFriendList(player.getUniqueId()).size() >= maxFriends) {
                new NoxetErrorMessage(NoxetErrorMessage.ErrorType.COMMON, "You have reached your friend limit.").send(player);
                return true;
            }

            if(new PlayerDataManager(player).doesContain(PlayerDataManager.Attribute.BLOCKED_PLAYERS, uuidToBefriend.toString())) {
                new NoxetErrorMessage(NoxetErrorMessage.ErrorType.COMMON, "You have blocked this player, and cannot send friend requests to them.").send(player);
                return true;
            }

            if(
                    new PlayerDataManager(uuidToBefriend).doesContain(PlayerDataManager.Attribute.BLOCKED_PLAYERS, player.getUniqueId().toString()) ||
                    (boolean) new PlayerDataManager(uuidToBefriend).get(PlayerDataManager.Attribute.DISALLOW_INCOMING_FRIEND_REQUESTS)
            ) {
                new NoxetErrorMessage(NoxetErrorMessage.ErrorType.COMMON, "You may not send friend requests to this player.").send(player);
                return true;
            }

            if(getFriendList(uuidToBefriend).size() >= maxFriends) {
                new NoxetErrorMessage(NoxetErrorMessage.ErrorType.COMMON, befriendName + " has reached their friend limit!").send(player);
                return true;
            }

            if(hasReceivedFriendRequestFrom(player.getUniqueId(), uuidToBefriend)) {
                acceptRequest(player.getUniqueId(), uuidToBefriend);

                new NoxetSuccessMessage("You are now friends with " + befriendName + "!").send(player);
                if(playerToBefriend != null)
                    new NoxetSuccessMessage(player.getName() + " accepted your friend request.").send(playerToBefriend);

                return true;
            }

            if(getOutgoingFriendRequests(player.getUniqueId()).size() >= maxOutgoingRequests) {
                new NoxetErrorMessage(NoxetErrorMessage.ErrorType.COMMON, "You have reached your limit on max amount of outgoing friend requests.").send(player);
                return true;
            }

            if(getIncomingFriendRequests(uuidToBefriend).size() >= maxIncomingRequests) {
                new NoxetErrorMessage(NoxetErrorMessage.ErrorType.COMMON, befriendName + " has enough friend requests to take care of, let them be!").send(player);
                return true;
            }

            sendFriendRequest(player.getUniqueId(), uuidToBefriend);

            new NoxetMessage("§eYou sent a friend request to " + befriendName + ".").send(player);

            if(playerToBefriend != null)
                new NoxetMessage("§e" + player.getName() + " sent you a friend request!")
                        .addButton("Accept", ChatColor.GREEN, "Become friends!", "friend add " + player.getName())
                        .addButton("Deny", ChatColor.RED, "Don't become friends", "friend deny " + player.getName())
                        .send(playerToBefriend);

            return true;
        }

        if(strings[0].equalsIgnoreCase("remove")) {
            if(strings.length < 2) {
                new NoxetErrorMessage(NoxetErrorMessage.ErrorType.ARGUMENT, "Missing argument: player to unfriend.").send(player);
                return true;
            }

            UUID uuidToUnfriend = new UsernameStorageManager().getUUIDFromUsernameOrUUID(strings[1]);

            if(uuidToUnfriend == null) {
                new NoxetErrorMessage(NoxetErrorMessage.ErrorType.COMMON, "That player is not registered.").send(player);
                return true;
            }

            Player playerToUnfriend = NoxetServer.getPlugin().getServer().getPlayer(uuidToUnfriend); // Only works if the player is online.
            String unfriendName = UsernameStorageManager.getCasedUsernameFromUUID(uuidToUnfriend); // This works anyway.

            if(!areFriends(player.getUniqueId(), uuidToUnfriend)) {
                new NoxetErrorMessage(NoxetErrorMessage.ErrorType.COMMON, "You are not friends!").send(player);
                return true;
            }

            removeFriend(player.getUniqueId(), uuidToUnfriend);

            new NoxetSuccessMessage(unfriendName + " is no longer your friend.").send(player);
            if(playerToUnfriend != null)
                new NoxetMessage("§c" + player.getName() + " removed you as their friend.").send(playerToUnfriend);

            return true;
        }

        if(strings[0].equalsIgnoreCase("deny")) {
            if(strings.length < 2) {
                new NoxetErrorMessage(NoxetErrorMessage.ErrorType.ARGUMENT, "Missing argument: player to deny.").send(player);
                return true;
            }

            UUID uuidToDeny = new UsernameStorageManager().getUUIDFromUsernameOrUUID(strings[1]);

            if(uuidToDeny == null) {
                new NoxetErrorMessage(NoxetErrorMessage.ErrorType.COMMON, "That player is not registered.").send(player);
                return true;
            }

            Player playerToDeny = NoxetServer.getPlugin().getServer().getPlayer(uuidToDeny); // Only works if the player is online.
            String denyName = UsernameStorageManager.getCasedUsernameFromUUID(uuidToDeny); // This works anyway.

            if(!hasReceivedFriendRequestFrom(player.getUniqueId(), uuidToDeny)) {
                new NoxetErrorMessage(NoxetErrorMessage.ErrorType.COMMON, "They have not sent you a friend request!").send(player);
                return true;
            }

            denyRequest(player.getUniqueId(), uuidToDeny);

            new NoxetSuccessMessage("Denied the friend request from " + denyName + ".").send(player);
            if(playerToDeny != null)
                new NoxetMessage("§c" + player.getName() + " denied your friend request.").send(playerToDeny);

            if(strings.length > 2 && strings[2].equals("!"))
                player.performCommand("block " + playerToDeny);

            return true;
        }

        if(strings[0].equalsIgnoreCase("cancel")) {
            if(strings.length < 2) {
                new NoxetErrorMessage(NoxetErrorMessage.ErrorType.ARGUMENT, "Missing argument: player to cancel request to.").send(player);
                return true;
            }

            UUID uuidToCancel = new UsernameStorageManager().getUUIDFromUsernameOrUUID(strings[1]);

            if(uuidToCancel == null) {
                new NoxetErrorMessage(NoxetErrorMessage.ErrorType.COMMON, "That player is not registered.").send(player);
                return true;
            }

            String cancelName = UsernameStorageManager.getCasedUsernameFromUUID(uuidToCancel); // This works anyway.

            if(!hasReceivedFriendRequestFrom(uuidToCancel, player.getUniqueId())) {
                new NoxetErrorMessage(NoxetErrorMessage.ErrorType.COMMON, "You have not sent them a friend request!").send(player);
                return true;
            }

            denyRequest(uuidToCancel, player.getUniqueId());

            new NoxetSuccessMessage("Canceled the friend request to " + cancelName + ".").send(player);

            return true;
        }

        if(strings[0].equalsIgnoreCase("list")) {
            List<String> friendUUIDs = getFriendList(player.getUniqueId());

            new NoxetMessage("§eFriends: " + friendUUIDs.size()).send(player);

            for(String friendUUIDString : friendUUIDs) {
                UUID friendUUID = new UsernameStorageManager().getUUIDFromUsernameOrUUID(friendUUIDString);
                String friendName = UsernameStorageManager.getCasedUsernameFromUUID(friendUUID);

                new NoxetMessage("└§a§lFRIEND §2" + friendName).send(player);
            }

            return true;
        }

        if(strings[0].equalsIgnoreCase("incoming")) {
            List<String> incomingUUIDs = getIncomingFriendRequests(player.getUniqueId());

            new NoxetMessage("§eIncoming friend requests: " + incomingUUIDs.size()).send(player);

            for(String incomingUUIDString : incomingUUIDs) {
                UUID incomingUUID = new UsernameStorageManager().getUUIDFromUsernameOrUUID(incomingUUIDString);
                String incomingName = UsernameStorageManager.getCasedUsernameFromUUID(incomingUUID);

                new NoxetMessage("└§6§lINCOMING §a" + incomingName)
                        .addButton("Accept", ChatColor.GREEN, "Become friends!", "friend add " + incomingUUIDString)
                        .addButton("Deny", ChatColor.RED, "Don't become friends", "friend deny " + incomingUUIDString)
                        .addButton("Deny and block", ChatColor.GRAY, "Deny this request and block " + incomingName, "friend deny " + incomingUUIDString + " !")
                        .send(player);
            }

            return true;
        }

        if(strings[0].equalsIgnoreCase("outgoing")) {
            List<String> outgoingUUIDs = getOutgoingFriendRequests(player.getUniqueId());

            new NoxetMessage("§eOutgoing friend requests: " + outgoingUUIDs.size()).send(player);

            for(String outgoingUUIDString : outgoingUUIDs) {
                UUID outgoingUUID = new UsernameStorageManager().getUUIDFromUsernameOrUUID(outgoingUUIDString);
                String outgoingName = UsernameStorageManager.getCasedUsernameFromUUID(outgoingUUID);

                new NoxetMessage("└§8§lOUTGOING §7" + outgoingName)
                        .addButton("Cancel", ChatColor.RED, "Regret wanting to become friends?", "friend cancel " + outgoingUUIDString)
                        .send(player);
            }

            return true;
        }

        if(strings[0].equalsIgnoreCase("toggle-allow-incoming")) {
            PlayerDataManager playerDataManager = new PlayerDataManager(player);
            boolean setTo = !(boolean) playerDataManager.get(PlayerDataManager.Attribute.DISALLOW_INCOMING_FRIEND_REQUESTS);
            playerDataManager.set(PlayerDataManager.Attribute.DISALLOW_INCOMING_FRIEND_REQUESTS, setTo).save();

            new NoxetSuccessMessage((setTo ? "Disabled" : "Enabled") + " incoming friend requests. Other players can " + (setTo ? "no longer" : "now") + " send friend requests to you.").send(player);

            if(setTo && playerDataManager.getListSize(PlayerDataManager.Attribute.INCOMING_FRIEND_REQUESTS) != 0)
                new NoxetNoteMessage("You have incoming friend requests already pending. If you want, you can cancel those manually.")
                        .addButton("Manage", ChatColor.YELLOW, "Manage incoming friend requests", "friend incoming")
                        .send(player);

            return true;
        }

        if(strings[0].equalsIgnoreCase("toggle-friend-tp")) {
            PlayerDataManager playerDataManager = new PlayerDataManager(player);
            boolean setTo = !(boolean) playerDataManager.get(PlayerDataManager.Attribute.FRIEND_TELEPORTATION);
            playerDataManager.set(PlayerDataManager.Attribute.FRIEND_TELEPORTATION, setTo).save();

            new NoxetSuccessMessage((setTo ? "Enabled" : "Disabled") + " friend teleportation. Your friends can " + (setTo ? "now" : "no longer") + " /tpa to you without you accepting.").send(player);

            return true;
        }

        new NoxetErrorMessage(NoxetErrorMessage.ErrorType.ARGUMENT, "Sorry, '" + strings[0] + "' is not a valid friend command.").send(player);

        return true;
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        if(!(commandSender instanceof Player))
            return null;

        List<String> completions = new ArrayList<>();

        Player player = (Player) commandSender;

        if(strings.length == 1) {
            completions.addAll(Arrays.asList("add", "remove", "deny", "list", "incoming", "outgoing", "toggle-allow-incoming", "toggle-friend-tp"));
        } else if(strings.length == 2) {
            switch(strings[0].toLowerCase()) {
                case "add":
                    Player playerToRecommend = NoxetServer.getPlugin().getServer().getPlayer(strings[1]);
                    if(playerToRecommend != null)
                        completions.add(playerToRecommend.getName());
                case "deny":
                    for(String incomingUUIDString : getIncomingFriendRequests(player.getUniqueId())) {
                        UUID incomingUUID = new UsernameStorageManager().getUUIDFromUsernameOrUUID(incomingUUIDString);
                        completions.add(incomingUUID != null ? UsernameStorageManager.getCasedUsernameFromUUID(incomingUUID) : incomingUUIDString);
                    }

                    break;
                case "remove":
                    for(String friendUUIDString : getFriendList(player.getUniqueId())) {
                        UUID friendUUID = new UsernameStorageManager().getUUIDFromUsernameOrUUID(friendUUIDString);
                        completions.add(friendUUID != null ? UsernameStorageManager.getCasedUsernameFromUUID(friendUUID) : friendUUIDString);
                    }

                    break;
                case "cancel":
                    for(String outgoingUUIDString : getOutgoingFriendRequests(player.getUniqueId())) {
                        UUID outgoingUUID = new UsernameStorageManager().getUUIDFromUsernameOrUUID(outgoingUUIDString);
                        completions.add(outgoingUUID != null ? UsernameStorageManager.getCasedUsernameFromUUID(outgoingUUID) : outgoingUUIDString);
                    }

                    break;
            }
        }

        return completions;
    }

    public static void sendFriendRequest(UUID from, UUID to) {
        new PlayerDataManager(from).addToStringList(PlayerDataManager.Attribute.OUTGOING_FRIEND_REQUESTS, to.toString()).save();
        new PlayerDataManager(to).addToStringList(PlayerDataManager.Attribute.INCOMING_FRIEND_REQUESTS, from.toString()).save();
    }

    public static boolean areFriends(UUID player1, UUID player2) {
        return getFriendList(player1).contains(player2.toString()) &&
                getFriendList(player2).contains(player1.toString());
    }

    @SuppressWarnings("unchecked")
    public static List<String> getFriendList(UUID uuid) {
        return (List<String>) new PlayerDataManager(uuid).get(PlayerDataManager.Attribute.FRIEND_LIST);
    }

    @SuppressWarnings("unchecked")
    public static List<String> getOutgoingFriendRequests(UUID uuid) {
        return (List<String>) new PlayerDataManager(uuid).get(PlayerDataManager.Attribute.OUTGOING_FRIEND_REQUESTS);
    }

    @SuppressWarnings("unchecked")
    public static List<String> getIncomingFriendRequests(UUID uuid) {
        return (List<String>) new PlayerDataManager(uuid).get(PlayerDataManager.Attribute.INCOMING_FRIEND_REQUESTS);
    }

    public static boolean hasReceivedFriendRequestFrom(UUID to, UUID from) {
        return getIncomingFriendRequests(to).contains(from.toString());
    }

    public static void acceptRequest(UUID to, UUID from) {
        new PlayerDataManager(to)
                .removeFromStringList(PlayerDataManager.Attribute.INCOMING_FRIEND_REQUESTS, from.toString())
                .addToStringList(PlayerDataManager.Attribute.FRIEND_LIST, from.toString())
                .save();
        new PlayerDataManager(from)
                .removeFromStringList(PlayerDataManager.Attribute.OUTGOING_FRIEND_REQUESTS, to.toString())
                .addToStringList(PlayerDataManager.Attribute.FRIEND_LIST, to.toString())
                .save();
    }

    public static void denyRequest(UUID to, UUID from) {
        new PlayerDataManager(to)
                .removeFromStringList(PlayerDataManager.Attribute.INCOMING_FRIEND_REQUESTS, from.toString())
                .save();
        new PlayerDataManager(from)
                .removeFromStringList(PlayerDataManager.Attribute.OUTGOING_FRIEND_REQUESTS, to.toString())
                .save();
    }

    public static void removeFriend(UUID player1, UUID player2) {
        new PlayerDataManager(player1)
                .removeFromStringList(PlayerDataManager.Attribute.FRIEND_LIST, player2.toString())
                .save();
        new PlayerDataManager(player2)
                .removeFromStringList(PlayerDataManager.Attribute.FRIEND_LIST, player1.toString())
                .save();
    }
}
