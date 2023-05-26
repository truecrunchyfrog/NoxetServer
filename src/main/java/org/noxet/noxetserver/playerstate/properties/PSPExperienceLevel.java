package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.entity.Player;
import org.noxet.noxetserver.playerstate.PlayerStateProperty;

public class PSPExperienceLevel implements PlayerStateProperty<Integer> {
    @Override
    public String getConfigName() {
        return "experience_level";
    }

    @Override
    public Integer getDefaultSerializedProperty() {
        return 0;
    }

    @Override
    public Integer getSerializedPropertyFromPlayer(Player player) {
        return player.getLevel();
    }

    @Override
    public void restoreProperty(Player player, Integer level) {
        player.setLevel(level);
    }
}
