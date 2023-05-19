package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.noxet.noxetserver.playerstate.PlayerStateProperty;

import java.util.List;

public class PSPPotionEffects extends PlayerStateProperty {
    @Override
    public String getConfigName() {
        return "potion_effects";
    }

    @Override
    public Object getSerializedPropertyFromPlayer(Player player) {
        return player.getActivePotionEffects().toArray(new PotionEffect[] {});
    }

    @Override
    @SuppressWarnings("unchecked")
    public void restoreProperty(Player player, Object value) {
        List<PotionEffect> potionEffectList = (List<PotionEffect>) value;

        for(PotionEffect potionEffect : potionEffectList)
            player.addPotionEffect(potionEffect);
    }
}
