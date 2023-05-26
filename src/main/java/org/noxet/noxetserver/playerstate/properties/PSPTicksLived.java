package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.entity.Player;
import org.noxet.noxetserver.playerstate.PlayerStateProperty;

public class PSPTicksLived implements PlayerStateProperty<Integer> {
    @Override
    public String getConfigName() {
        return "ticks_lived";
    }

    @Override
    public Integer getDefaultSerializedProperty() {
        return 1;
    }

    @Override
    public Integer getSerializedPropertyFromPlayer(Player player) {
        return player.getTicksLived();
    }

    @Override
    public void restoreProperty(Player player, Integer ticks) {
        player.setTicksLived(ticks);
    }
}
