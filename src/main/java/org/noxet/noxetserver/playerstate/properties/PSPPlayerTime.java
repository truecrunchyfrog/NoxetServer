package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.entity.Player;
import org.noxet.noxetserver.playerstate.PlayerStateProperty;

public class PSPPlayerTime implements PlayerStateProperty<Long> {
    @Override
    public String getConfigName() {
        return "player_time";
    }

    @Override
    public Long getDefaultSerializedProperty() {
        return null; // This value shouldn't be saved.
    }

    @Override
    public Long getSerializedPropertyFromPlayer(Player player) {
        return null; // This value shouldn't be saved.
    }

    @Override
    public void restoreProperty(Player player, Long value) {
        player.resetPlayerTime(); // Just reset.
    }
}
