package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.entity.Player;
import org.noxet.noxetserver.playerstate.PlayerStateProperty;

public class PSPHeldItemSlot implements PlayerStateProperty<Integer> {
    @Override
    public String getConfigName() {
        return "held_item_slot";
    }

    @Override
    public Integer getDefaultSerializedProperty() {
        return 0;
    }

    @Override
    public Integer getSerializedPropertyFromPlayer(Player player) {
        return player.getInventory().getHeldItemSlot();
    }

    @Override
    public void restoreProperty(Player player, Integer slot) {
        player.getInventory().setHeldItemSlot(slot);
    }
}
