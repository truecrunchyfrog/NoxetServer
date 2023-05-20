package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.entity.Player;
import org.noxet.noxetserver.playerstate.PlayerStateProperty;

public class PSPHealthScale extends PlayerStateProperty {
    @Override
    public String getConfigName() {
        return "health_scale";
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
