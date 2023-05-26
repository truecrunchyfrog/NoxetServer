package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.noxet.noxetserver.playerstate.PlayerStateProperty;

public class PSPOffHand implements PlayerStateProperty<ItemStack> {
    @Override
    public String getConfigName() {
        return "off_hand";
    }

    @Override
    public ItemStack getDefaultSerializedProperty() {
        return new ItemStack(Material.AIR);
    }

    @Override
    public ItemStack getSerializedPropertyFromPlayer(Player player) {
        return player.getInventory().getItemInOffHand();
    }

    @Override
    public void restoreProperty(Player player, ItemStack offHand) {
        player.getInventory().setItemInOffHand(offHand);
    }
}
