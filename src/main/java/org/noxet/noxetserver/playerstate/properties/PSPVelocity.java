package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.noxet.noxetserver.playerstate.PlayerStateProperty;

public class PSPVelocity implements PlayerStateProperty<Vector> {
    @Override
    public String getConfigName() {
        return "velocity";
    }

    @Override
    public Vector getDefaultSerializedProperty() {
        return new Vector();
    }

    @Override
    public Vector getSerializedPropertyFromPlayer(Player player) {
        return player.getVelocity();
    }

    @Override
    public void restoreProperty(Player player, Vector velocity) {
        player.setVelocity(velocity);
    }
}
