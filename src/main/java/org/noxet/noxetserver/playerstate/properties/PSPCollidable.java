package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.entity.Player;
import org.noxet.noxetserver.playerstate.PlayerStateProperty;

public class PSPCollidable implements PlayerStateProperty<Boolean> {
    @Override
    public String getConfigName() {
        return "collidable";
    }

    @Override
    public Boolean getDefaultSerializedProperty() {
        return true;
    }

    @Override
    public Boolean getSerializedPropertyFromPlayer(Player player) {
        return player.isCollidable();
    }

    @Override
    public void restoreProperty(Player player, Boolean collidable) {
        player.setCollidable(collidable);
    }
}
