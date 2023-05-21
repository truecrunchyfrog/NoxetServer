package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.entity.Player;
import org.noxet.noxetserver.playerstate.PlayerStateProperty;

public class PSPArrowsInBody extends PlayerStateProperty {
    @Override
    public String getConfigName() {
        return "arrows_in_body";
    }

    @Override
    public Object getSerializedPropertyFromPlayer(Player player) {
        return player.getArrowsInBody();
    }

    @Override
    public void restoreProperty(Player player, Object value) {
        player.setArrowsInBody((int) value);
    }
}
