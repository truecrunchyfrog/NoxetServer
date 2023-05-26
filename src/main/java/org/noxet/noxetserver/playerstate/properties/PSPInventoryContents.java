package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.noxet.noxetserver.playerstate.PlayerStateProperty;

import java.util.Objects;

public class PSPInventoryContents implements PlayerStateProperty<ItemStack[]> {
    @Override
    public String getConfigName() {
        return "inventory_contents";
    }

    @Override
    public ItemStack[] getDefaultSerializedProperty() {
        return new ItemStack[0];
    }

    @Override
    public ItemStack[] getSerializedPropertyFromPlayer(Player player) {
        return player.getInventory().getContents();
    }

    @Override
    public void restoreProperty(Player player, ItemStack[] contents) {
        player.getInventory().setContents(contents);
    }

    @Override
    @SuppressWarnings("SuspiciousToArrayCall")
    public ItemStack[] getValueFromConfig(ConfigurationSection config) {
        return Objects.requireNonNull(config.getList(getConfigName())).toArray(new ItemStack[0]);
    }
}
