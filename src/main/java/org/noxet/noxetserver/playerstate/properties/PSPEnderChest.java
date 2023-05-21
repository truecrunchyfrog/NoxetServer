package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;

public class PSPEnderChest extends _PlayerStateProperty {
    @Override
    public String getConfigName() {
        return "ender_chest";
    }

    @Override
    public Object getDefaultSerializedProperty() {
        return new ItemStack[0];
    }

    @Override
    public Object getSerializedPropertyFromPlayer(Player player) {
        return player.getEnderChest().getContents();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void restoreProperty(Player player, Object value) {
        player.getEnderChest().setContents(((ArrayList<ItemStack>) value).toArray(new ItemStack[0]));
    }
}
