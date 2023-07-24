package org.noxet.noxetserver.menus.inventory;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.noxet.noxetserver.NoxetServer;
import org.noxet.noxetserver.util.UsernameStorageManager;
import org.noxet.noxetserver.menus.ItemGenerator;
import org.noxet.noxetserver.playerdata.PlayerDataManager;
import org.noxet.noxetserver.util.InventoryCoordinate;
import org.noxet.noxetserver.util.InventoryCoordinateUtil;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class IncomingFriendRequestsMenu extends InventoryMenu {
    private final List<String> incomingPlayers;

    public IncomingFriendRequestsMenu(Player player) {
        super((new PlayerDataManager(player).getListSize(PlayerDataManager.Attribute.BLOCKED_PLAYERS) - 1) / 9 + 1, "✉ Incoming Friend Requests", false);
        //noinspection unchecked
        incomingPlayers = (List<String>) new PlayerDataManager(player.getUniqueId()).get(PlayerDataManager.Attribute.INCOMING_FRIEND_REQUESTS);
    }

    @Override
    protected void updateInventory() {
        for(String incomingUUIDString : incomingPlayers) {
            UUID incomingUUID = new UsernameStorageManager().getUUIDFromUsernameOrUUID(incomingUUIDString);
            String incomingName = UsernameStorageManager.getCasedUsernameFromUUID(incomingUUID);

            setSlotItem(
                    ItemGenerator.generatePlayerSkull(
                            NoxetServer.getPlugin().getServer().getOfflinePlayer(incomingUUID),
                            "§e" + (incomingName != null ? incomingName : incomingUUIDString),
                            Arrays.asList(
                                    "§7This player wants to befriend you.",
                                    "§e→ Double-click to §a§naccept§e.",
                                    "§e→ Shift-click to §c§ndeny§e.",
                                    "§e→ Press any number on keyboard to §8§nblock§e this player."
                            )
                    ),
                    InventoryCoordinateUtil.getCoordinateFromSlotIndex(incomingPlayers.indexOf(incomingUUIDString))
            );
        }
    }

    @Override
    protected boolean onSlotClick(Player player, InventoryCoordinate coordinate, ClickType clickType) {
        if(coordinate.getSlotIndex() > incomingPlayers.size() - 1)
            return false;

        String clickedIncomingUUID = incomingPlayers.get(coordinate.getSlotIndex());
        String clickedIncomingName = UsernameStorageManager.getCasedUsernameFromUUID(new UsernameStorageManager().getUUIDFromUsernameOrUUID(clickedIncomingUUID));

        switch(clickType) {
            case DOUBLE_CLICK:
                player.performCommand("friend add " + clickedIncomingUUID);
                break;
            case SHIFT_LEFT:
                player.performCommand("friend deny " + clickedIncomingUUID);
                break;
            case NUMBER_KEY:
                new ConfirmationMenu("Block '" + clickedIncomingName + "'?", () -> {
                    player.performCommand("block " + clickedIncomingUUID);
                    new IncomingFriendRequestsMenu(player).openInventory(player);
                }, () -> new IncomingFriendRequestsMenu(player).openInventory(player)).openInventory(player);
                return true;
        }

        new IncomingFriendRequestsMenu(player).openInventory(player);

        return true;
    }
}
