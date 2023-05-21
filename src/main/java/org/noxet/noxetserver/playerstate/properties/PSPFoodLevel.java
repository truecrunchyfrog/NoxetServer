package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.entity.Player;

public class PSPFoodLevel extends _PlayerStateProperty {
    @Override
    public String getConfigName() {
        return "food_level";
    }

    @Override
    public Object getDefaultSerializedProperty() {
        return 20;
    }

    @Override
    public Object getSerializedPropertyFromPlayer(Player player) {
        return player.getFoodLevel();
    }

    @Override
    public void restoreProperty(Player player, Object value) {
        player.setFoodLevel((int) value);
    }
}
