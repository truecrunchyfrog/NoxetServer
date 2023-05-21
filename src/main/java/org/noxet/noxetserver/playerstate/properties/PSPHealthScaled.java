package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.entity.Player;

public class PSPHealthScaled extends _PlayerStateProperty {
    @Override
    public String getConfigName() {
        return "health_scaled";
    }

    @Override
    public Object getDefaultSerializedProperty() {
        return false;
    }

    @Override
    public Object getSerializedPropertyFromPlayer(Player player) {
        return player.isHealthScaled();
    }

    @Override
    public void restoreProperty(Player player, Object value) {
        player.setHealthScaled((boolean) value);
    }
}
