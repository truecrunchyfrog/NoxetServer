package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.entity.Player;
import org.noxet.noxetserver.playerstate.PlayerStateProperty;

public class PSPExperienceLevel extends PlayerStateProperty {
    @Override
    public String getConfigName() {
        return "experience_level";
    }

    @Override
    public Object getSerializedPropertyFromPlayer(Player player) {
        return player.getLevel();
    }

    @Override
    public void restoreProperty(Player player, Object value) {
        player.setLevel((int) value);
    }
}
