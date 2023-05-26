package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.noxet.noxetserver.playerstate.PlayerStateProperty;

public class PSPFallDistance implements PlayerStateProperty<Float> {
    @Override
    public String getConfigName() {
        return "fall_distance";
    }

    @Override
    public Float getDefaultSerializedProperty() {
        return 0F;
    }

    @Override
    public Float getSerializedPropertyFromPlayer(Player player) {
        return player.getFallDistance();
    }

    @Override
    public void restoreProperty(Player player, Float distance) {
        player.setFallDistance(distance);
    }

    @Override
    public Float getValueFromConfig(ConfigurationSection config) {
        return ((Double) config.getDouble(getConfigName())).floatValue();
    }
}
