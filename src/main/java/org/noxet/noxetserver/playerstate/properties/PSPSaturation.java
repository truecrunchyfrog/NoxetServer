package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.entity.Player;

public class PSPSaturation extends _PlayerStateProperty {
    @Override
    public String getConfigName() {
        return "saturation";
    }

    @Override
    public Object getDefaultSerializedProperty() {
        return 20;
    }

    @Override
    public Object getSerializedPropertyFromPlayer(Player player) {
        return player.getSaturation();
    }

    @Override
    public void restoreProperty(Player player, Object value) {
        player.setSaturation(((Double) value).floatValue());
    }
}
