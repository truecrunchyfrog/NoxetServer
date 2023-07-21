package org.noxet.noxetserver.menus.inventorysetups;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;

public abstract class InventorySetup {
    protected final Inventory inventory;

    public InventorySetup() {
        inventory = Bukkit.createInventory(null, InventoryType.PLAYER);
    }

    protected abstract void populateInventory();

    public void applyToPlayer(Player player) {
        populateInventory();
        player.getInventory().setContents(inventory.getContents());
    }
}
