package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.entity.Player;

public class PSPTicksLived extends _PlayerStateProperty {
    @Override
    public String getConfigName() {
        return "ticks_lived";
    }

    @Override
    public Object getDefaultSerializedProperty() {
        return 0;
    }

    @Override
    public Object getSerializedPropertyFromPlayer(Player player) {
        return player.getTicksLived();
    }

    @Override
    public void restoreProperty(Player player, Object value) {
        player.setTicksLived((int) value);
    }
}
