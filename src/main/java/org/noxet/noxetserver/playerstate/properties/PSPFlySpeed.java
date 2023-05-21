package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.entity.Player;
import org.noxet.noxetserver.playerstate.PlayerStateProperty;

public class PSPFlySpeed extends PlayerStateProperty {
    @Override
    public String getConfigName() {
        return "fly_speed";
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
