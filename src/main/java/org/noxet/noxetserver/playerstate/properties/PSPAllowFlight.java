package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.entity.Player;
import org.noxet.noxetserver.playerstate.PlayerStateProperty;

public class PSPAllowFlight extends PlayerStateProperty {
    @Override
    public String getConfigName() {
        return "allow_flight";
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
