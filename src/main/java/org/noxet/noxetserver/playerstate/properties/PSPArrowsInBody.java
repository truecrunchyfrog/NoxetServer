package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.entity.Player;
import org.noxet.noxetserver.playerstate.PlayerStateProperty;

public class PSPArrowsInBody implements PlayerStateProperty<Integer> {
    @Override
    public String getConfigName() {
        return "arrows_in_body";
    }

    @Override
    public Integer getDefaultSerializedProperty() {
        return 0;
    }

    @Override
    public Integer getSerializedPropertyFromPlayer(Player player) {
        return player.getArrowsInBody();
    }

    @Override
    public void restoreProperty(Player player, Integer arrows) {
        player.setArrowsInBody(arrows);
    }
}
