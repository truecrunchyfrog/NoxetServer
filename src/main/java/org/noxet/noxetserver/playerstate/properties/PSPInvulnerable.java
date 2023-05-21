package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.entity.Player;

public class PSPInvulnerable extends _PlayerStateProperty {
    @Override
    public String getConfigName() {
        return "invulnerable";
    }

    @Override
    public Object getDefaultSerializedProperty() {
        return false;
    }

    @Override
    public Object getSerializedPropertyFromPlayer(Player player) {
        return player.isInvulnerable();
    }

    @Override
    public void restoreProperty(Player player, Object value) {
        player.setInvulnerable((boolean) value);
    }
}
