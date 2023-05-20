package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.entity.Player;
import org.noxet.noxetserver.playerstate.PlayerStateProperty;

public class PSPFireTicks extends PlayerStateProperty {
    @Override
    public String getConfigName() {
        return "fire_ticks";
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
