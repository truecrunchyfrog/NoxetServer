package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.noxet.noxetserver.playerstate.PlayerStateProperty;

public class PSPHealthScale implements PlayerStateProperty<Double> {
    @Override
    public String getConfigName() {
        return "health_scale";
    }

    @Override
    public Double getDefaultSerializedProperty() {
        return 20D;
    }

    @Override
    public Double getSerializedPropertyFromPlayer(Player player) {
        return player.getHealthScale();
    }

    @Override
    public void restoreProperty(Player player, Double scale) {
        player.setHealthScale(scale);
    }

    @Override
    public Double getValueFromConfig(ConfigurationSection config) {
        return config.getDouble(getConfigName());
    }
}
