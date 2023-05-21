package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class PSPVelocity extends _PlayerStateProperty {
    @Override
    public String getConfigName() {
        return "velocity";
    }

    @Override
    public Object getDefaultSerializedProperty() {
        return new Vector();
    }

    @Override
    public Object getSerializedPropertyFromPlayer(Player player) {
        return player.getVelocity();
    }

    @Override
    public void restoreProperty(Player player, Object value) {
        player.setVelocity((Vector) value);
    }
}
