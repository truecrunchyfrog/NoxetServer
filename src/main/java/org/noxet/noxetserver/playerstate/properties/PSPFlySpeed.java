package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.entity.Player;

public class PSPFlySpeed extends _PlayerStateProperty {
    @Override
    public String getConfigName() {
        return "fly_speed";
    }

    @Override
    public Object getDefaultSerializedProperty() {
        return 1;
    }

    @Override
    public Object getSerializedPropertyFromPlayer(Player player) {
        return player.getFlySpeed();
    }

    @Override
    public void restoreProperty(Player player, Object value) {
        player.setFlySpeed(((Double) value).floatValue());
    }
}
