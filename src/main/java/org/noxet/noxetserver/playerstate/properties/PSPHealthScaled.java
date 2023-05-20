package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.entity.Player;
import org.noxet.noxetserver.playerstate.PlayerStateProperty;

public class PSPHealthScaled extends PlayerStateProperty {
    @Override
    public String getConfigName() {
        return "health_scaled";
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
