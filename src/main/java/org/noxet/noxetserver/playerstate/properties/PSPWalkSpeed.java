package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.entity.Player;
import org.noxet.noxetserver.playerstate.PlayerStateProperty;

public class PSPWalkSpeed extends PlayerStateProperty {
    @Override
    public String getConfigName() {
        return "walk_speed";
    }

    @Override
    public Object getSerializedPropertyFromPlayer(Player player) {
        return player.getWalkSpeed();
    }

    @Override
    public void restoreProperty(Player player, Object value) {
        player.setWalkSpeed(((Double) value).floatValue());
    }
}
