package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.entity.Player;

public class PSPFlying extends _PlayerStateProperty {
    @Override
    public String getConfigName() {
        return "flying";
    }

    @Override
    public Object getDefaultSerializedProperty() {
        return false;
    }

    @Override
    public Object getSerializedPropertyFromPlayer(Player player) {
        return player.isFlying();
    }

    @Override
    public void restoreProperty(Player player, Object value) {
        player.setFlying((boolean) value);
    }
}
