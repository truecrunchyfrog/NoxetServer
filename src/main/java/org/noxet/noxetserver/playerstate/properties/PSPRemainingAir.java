package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.entity.Player;
import org.noxet.noxetserver.playerstate.PlayerStateProperty;

public class PSPRemainingAir extends PlayerStateProperty {
    @Override
    public String getConfigName() {
        return "remaining_air";
    }

    @Override
    public Object getSerializedPropertyFromPlayer(Player player) {
        return player.getRemainingAir();
    }

    @Override
    public void restoreProperty(Player player, Object value) {
        player.setRemainingAir((int) value);
    }
}
