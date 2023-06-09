package org.noxet.noxetserver.menus.inventory;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.noxet.noxetserver.menus.ItemGenerator;
import org.noxet.noxetserver.util.InventoryCoordinate;

import java.util.Collections;

public class SocialMenu extends InventoryMenu {
    private final Player player;

    public SocialMenu(Player player) {
        super(3, "Social", false);
        this.player = player;
    }

    public enum SocialSlot implements InventoryCoordinate {
        FRIENDS(2, 1),
        BLOCKED(6, 1);

        private final int x, y;

        SocialSlot(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public int getX() {
            return x;
        }

        @Override
        public int getY() {
            return y;
        }
    }

    @Override
    protected void updateInventory() {
        setSlotItem(
                ItemGenerator.generatePlayerSkull(player, "§aFriends", Collections.singletonList("§eView friendships.")),
                SocialSlot.FRIENDS
        );

        setSlotItem(
                ItemGenerator.generateItem(Material.BEDROCK, "§cBlocked Players", Collections.singletonList("§eView players you have blocked.")),
                SocialSlot.BLOCKED
        );
    }

    @Override
    protected boolean onSlotClick(Player player, InventoryCoordinate coordinate, ClickType clickType) {
        if(coordinate.isAt(SocialSlot.FRIENDS)) {
            new FriendsMenu(player).openInventory(player);
        } else if(coordinate.isAt(SocialSlot.BLOCKED)) {
            new BlockedPlayersMenu(player).openInventory(player);
        } else return false;

        return true;
    }
}
