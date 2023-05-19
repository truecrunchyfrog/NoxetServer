package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.entity.Player;
import org.noxet.noxetserver.playerstate.PlayerStateProperty;

public class PSPInvulnerable extends PlayerStateProperty {
    @Override
    public String getConfigName() {
        return "invulnerable";
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
