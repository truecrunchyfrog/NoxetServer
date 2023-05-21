package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.entity.Player;

public class PSPRemainingAir extends _PlayerStateProperty {
    @Override
    public String getConfigName() {
        return "remaining_air";
    }

    @Override
    public Object getDefaultSerializedProperty() {
        return 20 * 10; // 1 bubble (of 10) per second (20 ticks).
    }

    @Override
    public Object getSerializedPropertyFromPlayer(Player player) {
        return player.getRemainingAir();
    }

    @Override
    public void restoreProperty(Player player, Object value) {
        player.setRemainingAir((int) value);
    }
}
