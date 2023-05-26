package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.entity.Player;
import org.noxet.noxetserver.playerstate.PlayerStateProperty;

public class PSPFoodLevel implements PlayerStateProperty<Integer> {
    @Override
    public String getConfigName() {
        return "food_level";
    }

    @Override
    public Integer getDefaultSerializedProperty() {
        return 20;
    }

    @Override
    public Integer getSerializedPropertyFromPlayer(Player player) {
        return player.getFoodLevel();
    }

    @Override
    public void restoreProperty(Player player, Integer level) {
        player.setFoodLevel(level);
    }
}
