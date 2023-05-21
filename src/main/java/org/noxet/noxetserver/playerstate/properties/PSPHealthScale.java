package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.entity.Player;

public class PSPHealthScale extends _PlayerStateProperty {
    @Override
    public String getConfigName() {
        return "health_scale";
    }

    @Override
    public Object getDefaultSerializedProperty() {
        return 20D;
    }

    @Override
    public Object getSerializedPropertyFromPlayer(Player player) {
        return player.getHealthScale();
    }

    @Override
    public void restoreProperty(Player player, Object value) {
        player.setHealthScale((double) value);
    }
}
