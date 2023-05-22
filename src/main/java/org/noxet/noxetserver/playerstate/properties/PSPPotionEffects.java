package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.noxet.noxetserver.playerstate.PlayerStateProperty;

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
    public Class<PotionEffect[]> getTypeClass() {
        return PotionEffect[].class;
    }
}
