package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.entity.Player;
import org.noxet.noxetserver.playerstate.PlayerStateProperty;

public class PSPInvulnerable implements PlayerStateProperty<Boolean> {
    @Override
    public String getConfigName() {
        return "invulnerable";
    }

    @Override
    public Boolean getDefaultSerializedProperty() {
        return false;
    }

    @Override
    public Boolean getSerializedPropertyFromPlayer(Player player) {
        return player.isInvulnerable();
    }

    @Override
    public void restoreProperty(Player player, Boolean invulnerable) {
        player.setInvulnerable(invulnerable);
    }
}
