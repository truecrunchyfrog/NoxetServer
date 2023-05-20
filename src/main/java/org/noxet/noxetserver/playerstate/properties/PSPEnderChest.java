package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.noxet.noxetserver.playerstate.PlayerStateProperty;

public class PSPEnderChest extends PlayerStateProperty {
    @Override
    public String getConfigName() {
        return "ender_chest";
    }

    @Override
    public Object getSerializedPropertyFromPlayer(Player player) {
        return player.getEnderChest().getContents();
    }

    @Override
    public void restoreProperty(Player player, Object value) {
        player.getEnderChest().setContents((ItemStack[]) value);
    }
}
