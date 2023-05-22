package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.entity.Player;
import org.noxet.noxetserver.playerstate.PlayerStateProperty;

public class PSPFlying implements PlayerStateProperty<Boolean> {
    @Override
    public String getConfigName() {
        return "flying";
    }

    @Override
    public Boolean getDefaultSerializedProperty() {
        return false;
    }

    @Override
    public Boolean getSerializedPropertyFromPlayer(Player player) {
        return player.isFlying();
    }

    @Override
    public void restoreProperty(Player player, Boolean flying) {
        player.setFlying(flying);
    }

    @Override
    public Class<Boolean> getTypeClass() {
        return Boolean.class;
    }
}
