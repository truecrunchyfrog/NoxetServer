package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.entity.Player;
import org.noxet.noxetserver.playerstate.PlayerStateProperty;

public class PSPCollidable extends PlayerStateProperty {
    @Override
    public String getConfigName() {
        return "collidable";
    }

    @Override
    public Object getSerializedPropertyFromPlayer(Player player) {
        return player.isCollidable();
    }

    @Override
    public void restoreProperty(Player player, Object value) {
        player.setCollidable((boolean) value);
    }
}
