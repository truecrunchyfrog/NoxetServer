package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.noxet.noxetserver.playerstate.PlayerStateProperty;

import java.util.Objects;

public class PSPInventoryArmor implements PlayerStateProperty<ItemStack[]> {
    @Override
    public String getConfigName() {
        return "armor";
    }

    @Override
    public ItemStack[] getDefaultSerializedProperty() {
        return new ItemStack[0];
    }

    @Override
    public ItemStack[] getSerializedPropertyFromPlayer(Player player) {
        return player.getInventory().getArmorContents();
    }

    @Override
    public void restoreProperty(Player player, ItemStack[] armor) {
        player.getInventory().setArmorContents(armor);
    }

    @Override
    @SuppressWarnings("SuspiciousToArrayCall")
    public ItemStack[] getValueFromConfig(ConfigurationSection config) {
        return Objects.requireNonNull(config.getList(getConfigName())).toArray(new ItemStack[0]);
    }
}
