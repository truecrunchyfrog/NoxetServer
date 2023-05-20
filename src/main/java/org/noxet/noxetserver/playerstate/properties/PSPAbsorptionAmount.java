package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.entity.Player;
import org.noxet.noxetserver.playerstate.PlayerStateProperty;

public class PSPAbsorptionAmount extends PlayerStateProperty {
    @Override
    public String getConfigName() {
        return "absorption_amount";
    }

    @Override
    public Object getSerializedPropertyFromPlayer(Player player) {
        return player.getAbsorptionAmount();
    }

    @Override
    public void restoreProperty(Player player, Object value) {
        player.setAbsorptionAmount((double) value);
    }
}
