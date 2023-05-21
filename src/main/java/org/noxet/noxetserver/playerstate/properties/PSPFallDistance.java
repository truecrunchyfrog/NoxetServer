package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.entity.Player;

public class PSPFallDistance extends _PlayerStateProperty {
    @Override
    public String getConfigName() {
        return "fall_distance";
    }

    @Override
    public Object getDefaultSerializedProperty() {
        return 0;
    }

    @Override
    public Object getSerializedPropertyFromPlayer(Player player) {
        return player.getFallDistance();
    }

    @Override
    public void restoreProperty(Player player, Object value) {
        player.setFallDistance(((Double) value).floatValue());
    }
}
