package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.entity.Player;
import org.noxet.noxetserver.playerstate.PlayerStateProperty;

public class PSPRemainingAir implements PlayerStateProperty<Integer> {
    @Override
    public String getConfigName() {
        return "remaining_air";
    }

    @Override
    public Integer getDefaultSerializedProperty() {
        return 360;
    }

    @Override
    public Integer getSerializedPropertyFromPlayer(Player player) {
        return player.getRemainingAir();
    }

    @Override
    public void restoreProperty(Player player, Integer ticks) {
        player.setRemainingAir(ticks);
    }
}
