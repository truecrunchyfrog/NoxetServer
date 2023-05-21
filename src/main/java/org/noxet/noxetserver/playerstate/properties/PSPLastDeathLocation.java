package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public class PSPLastDeathLocation extends _PlayerStateProperty {
    @Override
    public String getConfigName() {
        return "last_death_location";
    }

    @Override
    public Object getDefaultSerializedProperty() {
        return null;
    }

    @Override
    public Object getSerializedPropertyFromPlayer(Player player) {
        return player.getLastDeathLocation();
    }

    @Override
    public void restoreProperty(Player player, Object value) {
        player.setLastDeathLocation((Location) value);
    }
}
