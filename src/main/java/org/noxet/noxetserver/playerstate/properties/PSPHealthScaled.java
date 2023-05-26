package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.entity.Player;
import org.noxet.noxetserver.playerstate.PlayerStateProperty;

public class PSPHealthScaled implements PlayerStateProperty<Boolean> {
    @Override
    public String getConfigName() {
        return "health_scaled";
    }

    @Override
    public Boolean getDefaultSerializedProperty() {
        return false;
    }

    @Override
    public Boolean getSerializedPropertyFromPlayer(Player player) {
        return player.isHealthScaled();
    }

    @Override
    public void restoreProperty(Player player, Boolean scaled) {
        player.setHealthScaled(scaled);
    }
}
