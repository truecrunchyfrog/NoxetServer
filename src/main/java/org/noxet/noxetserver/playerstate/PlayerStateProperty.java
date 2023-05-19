package org.noxet.noxetserver.playerstate;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public abstract class PlayerStateProperty implements PSPInterface {
    public void addToConfiguration(ConfigurationSection configSection, Player player) {
        configSection.set(getConfigName(), getSerializedPropertyFromPlayer(player));
    }

    public void restoreProperty(ConfigurationSection configSection, Player player) {
        restoreProperty(player, configSection.get(getConfigName()));
    }
}
