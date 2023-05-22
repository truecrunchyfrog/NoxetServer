package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.noxet.noxetserver.playerstate.PlayerStateProperty;

import java.util.HashMap;
import java.util.Map;

public class PSPPlayerInventory implements PlayerStateProperty<Map<String, Object>> {
    @Override
    public String getConfigName() {
        return "inventory";
    }

    @Override
    public Map<String, Object> getDefaultSerializedProperty() {
        Map<String, Object> inventoryMap = new HashMap<>();
        inventoryMap.put("contents", new ItemStack[0]);
        inventoryMap.put("armor", new ItemStack[0]);
        inventoryMap.put("off_hand", null);

        return inventoryMap;
    }

    @Override
    public Map<String, Object> getSerializedPropertyFromPlayer(Player player) {
        PlayerInventory inventory = player.getInventory();

        Map<String, Object> inventoryMap = new HashMap<>();
        inventoryMap.put("contents", inventory.getContents());
        inventoryMap.put("armor", inventory.getArmorContents());
        inventoryMap.put("off_hand", inventory.getItemInOffHand());

        return inventoryMap;
    }

    @Override
    public void restoreProperty(Player player, Map<String, Object> inventoryValues) {
        PlayerInventory playerInventory = player.getInventory();

        playerInventory.setContents((ItemStack[]) inventoryValues.get("contents"));
        playerInventory.setArmorContents((ItemStack[]) inventoryValues.get("armor"));
        playerInventory.setItemInOffHand((ItemStack) inventoryValues.get("off_hand"));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<Map<String, Object>> getTypeClass() {
        return (Class<Map<String, Object>>) (Class<?>) HashMap.class;
    }
}
