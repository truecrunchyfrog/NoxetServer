package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.entity.Player;
import org.noxet.noxetserver.playerstate.PlayerStateProperty;

public class PSPCanPickupItems implements PlayerStateProperty<Boolean> {
    @Override
    public String getConfigName() {
        return "can_pickup_items";
    }

    @Override
    public Boolean getDefaultSerializedProperty() {
        return true;
    }

    @Override
    public Boolean getSerializedPropertyFromPlayer(Player player) {
        return player.getCanPickupItems();
    }

    @Override
    public void restoreProperty(Player player, Boolean canPickupItems) {
        player.setCanPickupItems(canPickupItems);
    }
}
