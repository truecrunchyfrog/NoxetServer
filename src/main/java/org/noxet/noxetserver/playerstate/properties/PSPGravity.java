package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.entity.Player;

public class PSPGravity extends _PlayerStateProperty {
    @Override
    public String getConfigName() {
        return "gravity";
    }

    @Override
    public Object getDefaultSerializedProperty() {
        return true;
    }

    @Override
    public Object getSerializedPropertyFromPlayer(Player player) {
        return player.hasGravity();
    }

    @Override
    public void restoreProperty(Player player, Object value) {
        player.setGravity((boolean) value);
    }
}
