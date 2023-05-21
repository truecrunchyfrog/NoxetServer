package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.entity.Player;

public class PSPHealth extends _PlayerStateProperty {
    @Override
    public String getConfigName() {
        return "health";
    }

    @Override
    public Object getDefaultSerializedProperty() {
        return 20D;
    }

    @Override
    public Object getSerializedPropertyFromPlayer(Player player) {
        return player.getHealth();
    }

    @Override
    public void restoreProperty(Player player, Object value) {
        player.setHealth((double) value);
    }
}
