package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.entity.Player;
import org.noxet.noxetserver.playerstate.PlayerStateProperty;

public class PSPInvisible extends PlayerStateProperty {
    @Override
    public String getConfigName() {
        return "invisible";
    }

    @Override
    public Object getSerializedPropertyFromPlayer(Player player) {
        return player.isInvisible();
    }

    @Override
    public void restoreProperty(Player player, Object value) {
        player.setInvisible((boolean) value);
    }
}
