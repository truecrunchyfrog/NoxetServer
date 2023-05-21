package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.entity.Player;

public class PSPWalkSpeed extends _PlayerStateProperty {
    @Override
    public String getConfigName() {
        return "walk_speed";
    }

    @Override
    public Object getDefaultSerializedProperty() {
        return 1;
    }

    @Override
    public Object getSerializedPropertyFromPlayer(Player player) {
        return player.getWalkSpeed();
    }

    @Override
    public void restoreProperty(Player player, Object value) {
        player.setWalkSpeed(((Double) value).floatValue());
    }
}
