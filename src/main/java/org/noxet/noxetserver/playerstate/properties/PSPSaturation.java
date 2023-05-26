package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.noxet.noxetserver.playerstate.PlayerStateProperty;

public class PSPSaturation implements PlayerStateProperty<Float> {
    @Override
    public String getConfigName() {
        return "saturation";
    }

    @Override
    public Float getDefaultSerializedProperty() {
        return 20F;
    }

    @Override
    public Float getSerializedPropertyFromPlayer(Player player) {
        return player.getSaturation();
    }

    @Override
    public void restoreProperty(Player player, Float saturation) {
        player.setSaturation(saturation);
    }

    @Override
    public Float getValueFromConfig(ConfigurationSection config) {
        return ((Double) config.getDouble(getConfigName())).floatValue();
    }
}
