package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.entity.Player;
import org.noxet.noxetserver.playerstate.PlayerStateProperty;

public class PSPFreezeTicks implements PlayerStateProperty<Integer> {
    @Override
    public String getConfigName() {
        return "freeze_ticks";
    }

    @Override
    public Integer getDefaultSerializedProperty() {
        return 0;
    }

    @Override
    public Integer getSerializedPropertyFromPlayer(Player player) {
        return player.getFreezeTicks();
    }

    @Override
    public void restoreProperty(Player player, Integer ticks) {
        player.setFreezeTicks(ticks);
    }
}
