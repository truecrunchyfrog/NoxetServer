package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.entity.Player;
import org.noxet.noxetserver.playerstate.PlayerStateProperty;

public class PSPExperienceProgress implements PlayerStateProperty<Float> {
    @Override
    public String getConfigName() {
        return "experience_progress";
    }

    @Override
    public Float getDefaultSerializedProperty() {
        return 0F;
    }

    @Override
    public Float getSerializedPropertyFromPlayer(Player player) {
        return player.getExp();
    }

    @Override
    public void restoreProperty(Player player, Float progress) {
        player.setExp(progress);
    }

    @Override
    public Class<Float> getTypeClass() {
        return Float.class;
    }
}
