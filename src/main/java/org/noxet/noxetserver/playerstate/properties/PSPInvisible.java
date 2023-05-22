package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.entity.Player;
import org.noxet.noxetserver.playerstate.PlayerStateProperty;

public class PSPInvisible implements PlayerStateProperty<Boolean> {
    @Override
    public String getConfigName() {
        return "invisible";
    }

    @Override
    public Boolean getDefaultSerializedProperty() {
        return false;
    }

    @Override
    public Boolean getSerializedPropertyFromPlayer(Player player) {
        return player.isInvisible();
    }

    @Override
    public void restoreProperty(Player player, Boolean invisible) {
        player.setInvisible(invisible);
    }

    @Override
    public Class<Boolean> getTypeClass() {
        return Boolean.class;
    }
}
