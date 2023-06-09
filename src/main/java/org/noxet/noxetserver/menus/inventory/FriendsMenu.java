package org.noxet.noxetserver.menus.inventory;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.noxet.noxetserver.NoxetServer;
import org.noxet.noxetserver.RealmManager;
import org.noxet.noxetserver.UsernameStorageManager;
import org.noxet.noxetserver.commands.misc.Friend;
import org.noxet.noxetserver.menus.ItemGenerator;
import org.noxet.noxetserver.menus.chat.ChatPromptMenu;
import org.noxet.noxetserver.messaging.NoxetNoteMessage;
import org.noxet.noxetserver.playerdata.PlayerDataManager;
import org.noxet.noxetserver.util.InventoryCoordinate;
import org.noxet.noxetserver.util.InventoryCoordinateUtil;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.noxet.noxetserver.RealmManager.getCurrentRealm;

public class FriendsMenu extends InventoryMenu {
    private final Player player;
    private final List<String> friends;
    private final InventoryCoordinate addFriendSlot, toggleAllowFriendRequests, toggleFriendTPSlot;

    public FriendsMenu(Player player) {
        super((new PlayerDataManager(player).getListSize(PlayerDataManager.Attribute.FRIEND_LIST) - 1) / 9 + 2, "Friends", false);
        this.player = player;
        friends = Friend.getFriendList(player.getUniqueId());

        int y = getInventory().getSize() / 9 - 1;
        addFriendSlot = InventoryCoordinateUtil.getCoordinateFromXY(0, y);
        toggleAllowFriendRequests = InventoryCoordinateUtil.getCoordinateFromXY(7, y);
        toggleFriendTPSlot = InventoryCoordinateUtil.getCoordinateFromXY(8, y);
    }

    @Override
    protected void updateInventory() {
        for(String friendUUIDString : friends) {
            UUID friendUUID = new UsernameStorageManager().getUUIDFromUsernameOrUUID(friendUUIDString);
            String friendName = UsernameStorageManager.getCasedUsernameFromUUID(friendUUID);

            Player friendPlayer = NoxetServer.getPlugin().getServer().getPlayer(friendUUID);
            RealmManager.Realm realm = null;
            if(friendPlayer != null)
                realm = getCurrentRealm(friendPlayer);

            setSlotItem(
                    ItemGenerator.generatePlayerSkull(
                            NoxetServer.getPlugin().getServer().getOfflinePlayer(friendUUID),
                            "§3" + (friendName != null ? friendName : friendUUIDString),
                            Arrays.asList(
                                    friendPlayer != null ? "§aOnline" + (realm != null ? "§e @ §6" + realm.getDisplayName() : "") : "§cOffline",
                                    friendPlayer != null ? "§e→ Double-click to §d§nmessage§e." : "§8Messaging unavailable right now.",
                                    friendPlayer != null && realm != null && realm.doesAllowTeleportationMethods() && realm.equals(getCurrentRealm(player)) ? "§e→ Shift-click to send §c§5teleportation request§e." : "§8Teleportation unavailable.",
                                    "§e→ Press any number on keyboard to §c§nremove§e as friend."
                            )
                    ),
                    InventoryCoordinateUtil.getCoordinateFromSlotIndex(friends.indexOf(friendUUIDString))
            );
        }

        setSlotItem(
                ItemGenerator.generateItem(
                        Material.PAPER,
                        1,
                        "§aAdd Friend",
                        Arrays.asList("§7Send a friend request", "§7to someone to remain", "§7in touch."),
                        true
                ), addFriendSlot
        );

        boolean allowFriendRequestsStatus = !(boolean) new PlayerDataManager(player).get(PlayerDataManager.Attribute.DISALLOW_INCOMING_FRIEND_REQUESTS);

        setSlotItem(
                ItemGenerator.generateItem(
                        allowFriendRequestsStatus ? Material.GREEN_WOOL : Material.RED_WOOL,
                        "§aIncoming Friend Requests",
                        Arrays.asList(
                                allowFriendRequestsStatus ? "§a§lENABLED" : "§c§lDISABLED",
                                "§7When disabled, players cannot",
                                "§7send friend requests to you.",
                                "§7Only you can send requests to",
                                "§7them (unless they disabled this too).",
                                "§7Recommended to disable if you are spammed.",
                                "§e→ Click to " + (allowFriendRequestsStatus ? "disable" : "enable") + ".")
                ), toggleAllowFriendRequests
        );

        boolean friendTeleportStatus = (boolean) new PlayerDataManager(player).get(PlayerDataManager.Attribute.FRIEND_TELEPORTATION);

        setSlotItem(
                ItemGenerator.generateItem(
                        friendTeleportStatus ? Material.GREEN_WOOL : Material.RED_WOOL,
                        "§aFriend Teleportation",
                        Arrays.asList(
                                friendTeleportStatus ? "§a§lENABLED" : "§c§lDISABLED",
                                "§7When enabled, friends who",
                                "§7/tpa to you will automatically",
                                "§7be teleported without waiting for",
                                "§7you to accept.",
                                "§4Only enable if you trust your friends.",
                                "§e→ Click to " + (friendTeleportStatus ? "disable" : "enable") + ".")
                ), toggleFriendTPSlot
        );
    }

    @Override
    protected boolean onSlotClick(Player player, InventoryCoordinate coordinate, ClickType clickType) {
        if(coordinate.isAt(addFriendSlot)) {
            new ChatPromptMenu("name of player to befriend", player, promptResponse -> {
                player.performCommand("friend add " + promptResponse.getMessage());
                new FriendsMenu(player).openInventory(player);
            });
            return true;
        }

        if(coordinate.isAt(toggleAllowFriendRequests)) {
            player.performCommand("friend toggle-allow-incoming");
            updateInventory();
            return false;
        }

        if(coordinate.isAt(toggleFriendTPSlot)) {
            player.performCommand("friend toggle-friend-tp");
            updateInventory();
            return false;
        }

        if(coordinate.getSlotIndex() > friends.size() - 1)
            return false;

        String clickedFriendUUID = friends.get(coordinate.getSlotIndex());

        UUID friendUUID = new UsernameStorageManager().getUUIDFromUsernameOrUUID(clickedFriendUUID);
        String friendName = UsernameStorageManager.getCasedUsernameFromUUID(friendUUID);

        Player friendPlayer = NoxetServer.getPlugin().getServer().getPlayer(friendUUID);

        switch(clickType) {
            case DOUBLE_CLICK: // Message friend
                if(friendPlayer != null)
                    player.performCommand("msg " + friendName);
                else return false;
                break;
            case SHIFT_LEFT: // TPA to friend
                if(friendPlayer == null)
                    return false;

                RealmManager.Realm friendRealm = getCurrentRealm(friendPlayer);
                if(friendRealm != null && friendRealm.doesAllowTeleportationMethods() && friendRealm.equals(getCurrentRealm(player)))
                    player.performCommand("tpa " + friendUUID);
                else return false;
                break;
            case NUMBER_KEY: // Remove friend
                new ConfirmationMenu(
                        "Remove friendship with '" + friendName + "'?",
                        () -> {
                            player.performCommand("friend remove " + friendUUID);
                            new FriendsMenu(player).openInventory(player);
                        },
                        () -> {
                            new NoxetNoteMessage(friendName + " is still your friend.").send(player);
                            new FriendsMenu(player).openInventory(player);
                        }
                ).openInventory(player);
                break;
            default:
                return false;
        }

        return true;
    }
}
