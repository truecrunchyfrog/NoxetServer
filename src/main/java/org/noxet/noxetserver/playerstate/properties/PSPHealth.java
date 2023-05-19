package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.entity.Player;
import org.noxet.noxetserver.playerstate.PlayerStateProperty;

public class PSPHealth extends PlayerStateProperty {
    @Override
    public String getConfigName() {
        return "health";
    }

    @Override
    public Object getSerializedPropertyFromPlayer(Player player) {
        return player.getHealth();
    }

    @Override
    public void restoreProperty(Player player, Object value) {
        player.setHealth((double) value);
    }
}
