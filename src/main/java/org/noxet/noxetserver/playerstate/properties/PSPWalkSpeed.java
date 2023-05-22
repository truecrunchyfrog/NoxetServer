package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.entity.Player;
import org.noxet.noxetserver.playerstate.PlayerStateProperty;

public class PSPWalkSpeed implements PlayerStateProperty<Float> {
    @Override
    public String getConfigName() {
        return "walk_speed";
    }

    @Override
    public Float getDefaultSerializedProperty() {
        return 0.2F;
    }

    @Override
    public Float getSerializedPropertyFromPlayer(Player player) {
        return player.getWalkSpeed();
    }

    @Override
    public void restoreProperty(Player player, Float speed) {
        player.setWalkSpeed(speed);
    }

    @Override
    public Class<Float> getTypeClass() {
        return Float.class;
    }
}
