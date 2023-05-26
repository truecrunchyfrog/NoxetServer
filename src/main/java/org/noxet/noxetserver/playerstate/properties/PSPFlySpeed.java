package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.noxet.noxetserver.playerstate.PlayerStateProperty;

public class PSPFlySpeed implements PlayerStateProperty<Float> {
    @Override
    public String getConfigName() {
        return "fly_speed";
    }

    @Override
    public Float getDefaultSerializedProperty() {
        return 0.1F;
    }

    @Override
    public Float getSerializedPropertyFromPlayer(Player player) {
        return player.getFlySpeed();
    }

    @Override
    public void restoreProperty(Player player, Float speed) {
        player.setFlySpeed(speed);
    }

    @Override
    public Float getValueFromConfig(ConfigurationSection config) {
        return ((Double) config.getDouble(getConfigName())).floatValue();
    }
}
