package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.entity.Player;

public class PSPFreezeTicks extends _PlayerStateProperty {
    @Override
    public String getConfigName() {
        return "freeze_ticks";
    }

    @Override
    public Object getDefaultSerializedProperty() {
        return 0;
    }

    @Override
    public Object getSerializedPropertyFromPlayer(Player player) {
        return player.getFreezeTicks();
    }

    @Override
    public void restoreProperty(Player player, Object value) {
        player.setFreezeTicks((int) value);
    }
}
