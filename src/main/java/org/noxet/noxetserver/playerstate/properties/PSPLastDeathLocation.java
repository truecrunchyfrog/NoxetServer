package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.noxet.noxetserver.playerstate.PlayerStateProperty;

public class PSPLastDeathLocation implements PlayerStateProperty<Location> {
    @Override
    public String getConfigName() {
        return "last_death_location";
    }

    @Override
    public Location getDefaultSerializedProperty() {
        return null;
    }

    @Override
    public Location getSerializedPropertyFromPlayer(Player player) {
        return player.getLastDeathLocation();
    }

    @Override
    public void restoreProperty(Player player, Location deathLocation) {
        player.setLastDeathLocation(deathLocation);
    }
}
