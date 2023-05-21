package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.entity.Player;
import org.noxet.noxetserver.playerstate.PlayerStateProperty;

public class PSPGravity extends PlayerStateProperty {
    @Override
    public String getConfigName() {
        return "gravity";
    }

    @Override
    public Object getSerializedPropertyFromPlayer(Player player) {
        return player.hasGravity();
    }

    @Override
    public void restoreProperty(Player player, Object value) {
        player.setGravity((boolean) value);
    }
}
