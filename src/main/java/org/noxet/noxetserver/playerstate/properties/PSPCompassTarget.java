package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.noxet.noxetserver.NoxetServer;
import org.noxet.noxetserver.playerstate.PlayerStateProperty;

public class PSPCompassTarget implements PlayerStateProperty<Location> {
    @Override
    public String getConfigName() {
        return "compass_target";
    }

    @Override
    public Location getDefaultSerializedProperty() {
        return NoxetServer.ServerWorld.HUB.getWorld().getSpawnLocation();
    }

    @Override
    public Location getSerializedPropertyFromPlayer(Player player) {
        return player.getCompassTarget();
    }

    @Override
    public void restoreProperty(Player player, Location target) {
        player.setCompassTarget(target);
    }

    @Override
    public Class<Location> getTypeClass() {
        return Location.class;
    }
}
