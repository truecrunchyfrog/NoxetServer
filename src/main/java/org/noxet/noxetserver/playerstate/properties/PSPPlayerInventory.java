package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.noxet.noxetserver.playerstate.PlayerStateProperty;

import java.util.HashMap;
import java.util.Map;

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
        Map<String, Object> inventoryValues = (Map<String, Object>) value;

        PlayerInventory playerInventory = player.getInventory();
        playerInventory.setContents((ItemStack[]) inventoryValues.get("contents"));
        playerInventory.setArmorContents((ItemStack[]) inventoryValues.get("armor"));
        playerInventory.setItemInOffHand((ItemStack) inventoryValues.get("off_hand"));
    }
}
