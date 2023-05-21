package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.entity.Player;

public class PSPCollidable extends _PlayerStateProperty {
    @Override
    public String getConfigName() {
        return "collidable";
    }

    @Override
    public Object getDefaultSerializedProperty() {
        return true;
    }

    @Override
    public Object getSerializedPropertyFromPlayer(Player player) {
        return player.isCollidable();
    }

    @Override
    public void restoreProperty(Player player, Object value) {
        player.setCollidable((boolean) value);
    }
}
