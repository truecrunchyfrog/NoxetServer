package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.entity.Player;

public class PSPAllowFlight extends _PlayerStateProperty {
    @Override
    public String getConfigName() {
        return "allow_flight";
    }

    @Override
    public Object getDefaultSerializedProperty() {
        return false;
    }

    @Override
    public Object getSerializedPropertyFromPlayer(Player player) {
        return player.getAllowFlight();
    }

    @Override
    public void restoreProperty(Player player, Object value) {
        player.setAllowFlight((boolean) value);
    }
}
