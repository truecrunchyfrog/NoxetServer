package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.entity.Player;
import org.noxet.noxetserver.playerstate.PlayerStateProperty;

public class PSPAbsorptionAmount implements PlayerStateProperty<Double> {
    @Override
    public String getConfigName() {
        return "absorption_amount";
    }

    @Override
    public Double getDefaultSerializedProperty() {
        return 0D;
    }

    @Override
    public Double getSerializedPropertyFromPlayer(Player player) {
        return player.getAbsorptionAmount();
    }

    @Override
    public void restoreProperty(Player player, Double absorption) {
        player.setAbsorptionAmount(absorption);
    }

    @Override
    public Class<Double> getTypeClass() {
        return Double.class;
    }
}
