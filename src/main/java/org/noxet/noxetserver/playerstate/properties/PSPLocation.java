package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.noxet.noxetserver.playerstate.PlayerStateProperty;

public class PSPLocation extends PlayerStateProperty {
    @Override
    public String getConfigName() {
        return "location";
    }

    @Override
    public Object getSerializedPropertyFromPlayer(Player player) {
        return player.getLocation();
    }

    @Override
    public void restoreProperty(Player player, Object value) {
        player.teleport((Location) value);
    }
}
