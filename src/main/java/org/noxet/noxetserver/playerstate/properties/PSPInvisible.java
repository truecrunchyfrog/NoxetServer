package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.entity.Player;

public class PSPInvisible extends _PlayerStateProperty {
    @Override
    public String getConfigName() {
        return "invisible";
    }

    @Override
    public Object getDefaultSerializedProperty() {
        return false;
    }

    @Override
    public Object getSerializedPropertyFromPlayer(Player player) {
        return player.isInvisible();
    }

    @Override
    public void restoreProperty(Player player, Object value) {
        player.setInvisible((boolean) value);
    }
}
