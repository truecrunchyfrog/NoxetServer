package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.entity.Player;
import org.noxet.noxetserver.playerstate.PlayerStateProperty;

public class PSPAllowFlight implements PlayerStateProperty<Boolean> {
    @Override
    public String getConfigName() {
        return "allow_flight";
    }

    @Override
    public Boolean getDefaultSerializedProperty() {
        return false;
    }

    @Override
    public Boolean getSerializedPropertyFromPlayer(Player player) {
        return player.getAllowFlight();
    }

    @Override
    public void restoreProperty(Player player, Boolean allow) {
        player.setAllowFlight(allow);
    }

    @Override
    public Class<Boolean> getTypeClass() {
        return Boolean.class;
    }
}
