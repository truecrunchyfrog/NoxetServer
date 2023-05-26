package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.noxet.noxetserver.playerstate.PlayerStateProperty;

import java.util.Objects;

public class PSPPotionEffects implements PlayerStateProperty<PotionEffect[]> {
    @Override
    public String getConfigName() {
        return "potion_effects";
    }

    @Override
    public PotionEffect[] getDefaultSerializedProperty() {
        return new PotionEffect[0];
    }

    @Override
    public PotionEffect[] getSerializedPropertyFromPlayer(Player player) {
        return player.getActivePotionEffects().toArray(new PotionEffect[0]);
    }

    @Override
    public void restoreProperty(Player player, PotionEffect[] potionEffectList) {
        for(PotionEffect potionEffect : potionEffectList)
            player.addPotionEffect(potionEffect);
    }

    @Override
    @SuppressWarnings("SuspiciousToArrayCall")
    public PotionEffect[] getValueFromConfig(ConfigurationSection config) {
        return Objects.requireNonNull(config.getList(getConfigName())).toArray(new PotionEffect[0]);
    }
}
