package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.noxet.noxetserver.playerstate.PlayerStateProperty;

public class PSPExhaustion implements PlayerStateProperty<Float> {
    @Override
    public String getConfigName() {
        return "exhaustion";
    }

    @Override
    public Float getDefaultSerializedProperty() {
        return 0F;
    }

    @Override
    public Float getSerializedPropertyFromPlayer(Player player) {
        return player.getExhaustion();
    }

    @Override
    public void restoreProperty(Player player, Float exhaustion) {
        player.setExhaustion(exhaustion);
    }

    @Override
    public Float getValueFromConfig(ConfigurationSection config) {
        return ((Double) config.getDouble(getConfigName())).floatValue();
    }
}
