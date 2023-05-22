package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.noxet.noxetserver.playerstate.PlayerStateProperty;

public class PSPBedSpawnLocation implements PlayerStateProperty<Location> {
    @Override
    public String getConfigName() {
        return "bed_spawn_location";
    }

    @Override
    public Location getDefaultSerializedProperty() {
        return null;
    }

    @Override
    public Location getSerializedPropertyFromPlayer(Player player) {
        return player.getBedSpawnLocation();
    }

    @Override
    public void restoreProperty(Player player, Location spawnLocation) {
        player.setBedSpawnLocation(spawnLocation);
    }

    @Override
    public Class<Location> getTypeClass() {
        return Location.class;
    }
}
