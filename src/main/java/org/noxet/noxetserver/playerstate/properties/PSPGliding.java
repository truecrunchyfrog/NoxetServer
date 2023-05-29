package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.entity.Player;
import org.noxet.noxetserver.playerstate.PlayerStateProperty;

public class PSPGliding implements PlayerStateProperty<Boolean> {
    @Override
    public String getConfigName() {
        return "gliding";
    }

    @Override
    public Boolean getDefaultSerializedProperty() {
        return false;
    }

    @Override
    public Boolean getSerializedPropertyFromPlayer(Player player) {
        return player.isGliding();
    }

    @Override
    public void restoreProperty(Player player, Boolean gliding) {
        player.setGliding(gliding);
    }
}
