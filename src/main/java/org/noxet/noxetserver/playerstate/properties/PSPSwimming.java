package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.entity.Player;
import org.noxet.noxetserver.playerstate.PlayerStateProperty;

public class PSPSwimming implements PlayerStateProperty<Boolean> {
    @Override
    public String getConfigName() {
        return "swimming";
    }

    @Override
    public Boolean getDefaultSerializedProperty() {
        return false;
    }

    @Override
    public Boolean getSerializedPropertyFromPlayer(Player player) {
        return player.isSwimming();
    }

    @Override
    public void restoreProperty(Player player, Boolean swimming) {
        player.setSwimming(swimming);
    }
}
