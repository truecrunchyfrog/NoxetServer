package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.entity.Player;
import org.noxet.noxetserver.playerstate.PlayerStateProperty;

public class PSPGravity implements PlayerStateProperty<Boolean> {
    @Override
    public String getConfigName() {
        return "gravity";
    }

    @Override
    public Boolean getDefaultSerializedProperty() {
        return true;
    }

    @Override
    public Boolean getSerializedPropertyFromPlayer(Player player) {
        return player.hasGravity();
    }

    @Override
    public void restoreProperty(Player player, Boolean gravity) {
        player.setGravity(gravity);
    }
}
