package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.noxet.noxetserver.playerstate.PlayerStateProperty;

public class PSPCompassTarget extends PlayerStateProperty {
    @Override
    public String getConfigName() {
        return "compass_target";
    }

    @Override
    public Object getSerializedPropertyFromPlayer(Player player) {
        return player.getCompassTarget();
    }

    @Override
    public void restoreProperty(Player player, Object value) {
        player.setCompassTarget((Location) value);
    }
}
