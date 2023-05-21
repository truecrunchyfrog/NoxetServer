package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public class PSPBedSpawnLocation extends _PlayerStateProperty {
    @Override
    public String getConfigName() {
        return "bed_spawn_location";
    }

    @Override
    public Object getDefaultSerializedProperty() {
        return null;
    }

    @Override
    public Object getSerializedPropertyFromPlayer(Player player) {
        return player.getBedSpawnLocation();
    }

    @Override
    public void restoreProperty(Player player, Object value) {
        player.setBedSpawnLocation((Location) value);
    }
}
