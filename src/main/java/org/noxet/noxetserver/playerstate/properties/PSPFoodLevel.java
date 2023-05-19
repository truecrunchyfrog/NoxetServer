package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.entity.Player;
import org.noxet.noxetserver.playerstate.PlayerStateProperty;

public class PSPFoodLevel extends PlayerStateProperty {
    @Override
    public String getConfigName() {
        return "food_level";
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
