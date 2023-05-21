package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.entity.Player;
import org.noxet.noxetserver.playerstate.PlayerStateProperty;

public class PSPFallDistance extends PlayerStateProperty {
    @Override
    public String getConfigName() {
        return "fall_distance";
    }

    @Override
    public Object getSerializedPropertyFromPlayer(Player player) {
        return player.getFallDistance();
    }

    @Override
    public void restoreProperty(Player player, Object value) {
        player.setFallDistance(((Double) value).floatValue());
    }
}
