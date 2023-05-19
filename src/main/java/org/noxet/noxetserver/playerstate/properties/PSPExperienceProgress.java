package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.entity.Player;
import org.noxet.noxetserver.playerstate.PlayerStateProperty;

public class PSPExperienceProgress extends PlayerStateProperty {
    @Override
    public String getConfigName() {
        return "experience_progress";
    }

    @Override
    public Object getSerializedPropertyFromPlayer(Player player) {
        return player.getExp();
    }

    @Override
    public void restoreProperty(Player player, Object value) {
        player.setExp((float) value);
    }
}
