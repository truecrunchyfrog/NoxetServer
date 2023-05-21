package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.configuration.MemorySection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.noxet.noxetserver.playerstate.PlayerStateProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class PSPPlayerInventory extends PlayerStateProperty {
    @Override
    public String getConfigName() {
        return "inventory";
    }

    @Override
    public Object getSerializedPropertyFromPlayer(Player player) {
        PlayerInventory inventory = player.getInventory();

        Map<String, Object> inventoryMap = new HashMap<>();
        inventoryMap.put("contents", inventory.getContents());
        inventoryMap.put("armor", inventory.getArmorContents());
        inventoryMap.put("off_hand", inventory.getItemInOffHand());

        return inventoryMap;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void restoreProperty(Player player, Object value) {
        MemorySection inventoryValues = (MemorySection) value;

        PlayerInventory playerInventory = player.getInventory();
        playerInventory.setContents(((ArrayList<ItemStack>) Objects.requireNonNull(inventoryValues.get("contents"))).toArray(new ItemStack[0]));
        playerInventory.setArmorContents(((ArrayList<ItemStack>) Objects.requireNonNull(inventoryValues.get("armor"))).toArray(new ItemStack[0]));
        playerInventory.setItemInOffHand((ItemStack) inventoryValues.get("off_hand"));
    }
}
