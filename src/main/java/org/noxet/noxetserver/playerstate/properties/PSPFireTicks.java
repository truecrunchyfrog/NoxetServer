package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.entity.Player;

public class PSPFireTicks extends _PlayerStateProperty {
    @Override
    public String getConfigName() {
        return "fire_ticks";
    }

    @Override
    public Object getDefaultSerializedProperty() {
        return 0;
    }

    @Override
    public Object getSerializedPropertyFromPlayer(Player player) {
        return player.getFireTicks();
    }

    @Override
    public void restoreProperty(Player player, Object value) {
        player.setFireTicks((int) value);
    }
}
