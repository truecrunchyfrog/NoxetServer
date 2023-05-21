package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.entity.Player;

public class PSPExperienceProgress extends _PlayerStateProperty {
    @Override
    public String getConfigName() {
        return "experience_progress";
    }

    @Override
    public Object getDefaultSerializedProperty() {
        return 0;
    }

    @Override
    public Object getSerializedPropertyFromPlayer(Player player) {
        return player.getExp();
    }

    @Override
    public void restoreProperty(Player player, Object value) {
        player.setExp(((Double) value).floatValue());
    }
}
