package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.entity.Player;

public class PSPExperienceLevel extends _PlayerStateProperty {
    @Override
    public String getConfigName() {
        return "experience_level";
    }

    @Override
    public Object getDefaultSerializedProperty() {
        return 0;
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
