package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.noxet.noxetserver.playerstate.PlayerStateProperty;

public class PSPVelocity extends PlayerStateProperty {
    @Override
    public String getConfigName() {
        return "velocity";
    }

    @Override
    public Object getSerializedPropertyFromPlayer(Player player) {
        return player.getVelocity();
    }

    @Override
    public void restoreProperty(Player player, Object value) {
        player.setVelocity((Vector) value);
    }
}
