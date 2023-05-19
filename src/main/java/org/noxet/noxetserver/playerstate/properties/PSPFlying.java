package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.entity.Player;
import org.noxet.noxetserver.playerstate.PlayerStateProperty;

public class PSPFlying extends PlayerStateProperty {
    @Override
    public String getConfigName() {
        return "flying";
    }

    @Override
    public Object getSerializedPropertyFromPlayer(Player player) {
        return player.isFlying();
    }

    @Override
    public void restoreProperty(Player player, Object value) {
        player.setFlying((boolean) value);
    }
}
