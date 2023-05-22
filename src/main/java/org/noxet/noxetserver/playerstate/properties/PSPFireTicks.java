package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.entity.Player;
import org.noxet.noxetserver.playerstate.PlayerStateProperty;

public class PSPFireTicks implements PlayerStateProperty<Integer> {
    @Override
    public String getConfigName() {
        return "fire_ticks";
    }

    @Override
    public Integer getDefaultSerializedProperty() {
        return 0;
    }

    @Override
    public Integer getSerializedPropertyFromPlayer(Player player) {
        return player.getFireTicks();
    }

    @Override
    public void restoreProperty(Player player, Integer ticks) {
        player.setFireTicks(ticks);
    }

    @Override
    public Class<Integer> getTypeClass() {
        return Integer.class;
    }
}
