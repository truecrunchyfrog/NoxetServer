package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.noxet.noxetserver.playerstate.PlayerStateProperty;

public class PSPHealth implements PlayerStateProperty<Double> {
    @Override
    public String getConfigName() {
        return "health";
    }

    @Override
    public Double getDefaultSerializedProperty() {
        return 20D;
    }

    @Override
    public Double getSerializedPropertyFromPlayer(Player player) {
        return player.getHealth();
    }

    @Override
    public void restoreProperty(Player player, Double health) {
        player.setHealth(health);
    }

    @Override
    public Double getValueFromConfig(ConfigurationSection config) {
        return config.getDouble(getConfigName());
    }
}
