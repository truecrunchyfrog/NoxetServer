package org.noxet.noxetserver.menus.inventory;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.noxet.noxetserver.NoxetServer;
import org.noxet.noxetserver.UsernameStorageManager;
import org.noxet.noxetserver.menus.ItemGenerator;
import org.noxet.noxetserver.menus.chat.ChatPromptMenu;
import org.noxet.noxetserver.playerdata.PlayerDataManager;
import org.noxet.noxetserver.util.InventoryCoordinate;
import org.noxet.noxetserver.util.InventoryCoordinateUtil;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class BlockedPlayersMenu extends InventoryMenu {
    private final List<String> blockedPlayers;
    private final InventoryCoordinate blockNewPlayerSlot;

    public BlockedPlayersMenu(Player player) {
        super((new PlayerDataManager(player).getListSize(PlayerDataManager.Attribute.BLOCKED_PLAYERS) - 1) / 9 + 2, "Blocked Players", false);
        //noinspection unchecked
        blockedPlayers = (List<String>) new PlayerDataManager(player.getUniqueId()).get(PlayerDataManager.Attribute.BLOCKED_PLAYERS);

        blockNewPlayerSlot = InventoryCoordinateUtil.getCoordinateFromXY(0, getInventory().getSize() / 9 - 1);
    }

    @Override
    protected void updateInventory() {
        for(String blockedUUIDString : blockedPlayers) {
            UUID blockedUUID = new UsernameStorageManager().getUUIDFromUsernameOrUUID(blockedUUIDString);
            String blockedName = UsernameStorageManager.getCasedUsernameFromUUID(blockedUUID);

            setSlotItem(
                    ItemGenerator.generatePlayerSkull(
                            NoxetServer.getPlugin().getServer().getOfflinePlayer(blockedUUID),
                            "§c" + (blockedName != null ? blockedName : blockedUUIDString),
                            Arrays.asList(
                                    "§7UUID: " + blockedUUIDString,
                                    "§e→ Double-click to §a§npardon§e."
                            )
                    ),
                    InventoryCoordinateUtil.getCoordinateFromSlotIndex(blockedPlayers.indexOf(blockedUUIDString))
            );
        }

        setSlotItem(
                ItemGenerator.generateItem(
                        Material.PAPER,
                        1,
                        "§aBlock Player",
                        Arrays.asList("§7Block a player to", "§7prevent them from sending", "§7messages and friend/TPA requests."),
                        true
                ), blockNewPlayerSlot
        );
    }

    @Override
    protected boolean onSlotClick(Player player, InventoryCoordinate coordinate, ClickType clickType) {
        if(coordinate.isAt(blockNewPlayerSlot)) {
            new ChatPromptMenu("player to block", player, promptResponse -> {
                player.performCommand("block " + promptResponse.getMessage());
                new BlockedPlayersMenu(player).openInventory(player);
            });
            return true;
        }

        if(clickType != ClickType.DOUBLE_CLICK)
            return false;

        if(coordinate.getSlotIndex() > blockedPlayers.size() - 1)
            return false;

        String clickedBlockedUUID = blockedPlayers.get(coordinate.getSlotIndex());

        player.performCommand("unblock " + clickedBlockedUUID);

        new BlockedPlayersMenu(player).openInventory(player);

        return true;
    }
}
