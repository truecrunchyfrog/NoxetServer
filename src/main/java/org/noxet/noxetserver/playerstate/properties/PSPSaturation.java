package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.entity.Player;
import org.noxet.noxetserver.playerstate.PlayerStateProperty;

public class PSPSaturation extends PlayerStateProperty {
    @Override
    public String getConfigName() {
        return "saturation";
    }

    @Override
    public Object getSerializedPropertyFromPlayer(Player player) {
        return player.getSaturation();
    }

    @Override
    public void restoreProperty(Player player, Object value) {
        player.setSaturation((float) value);
    }
}
