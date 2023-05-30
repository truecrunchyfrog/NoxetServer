package org.noxet.noxetserver.playerstate;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.noxet.noxetserver.playerstate.properties.*;

public class PSPManager {
    private static final PlayerStateProperty<?>[] properties = {
            new PSPAbsorptionAmount(),
            new PSPAdvancementCriteria(),
            new PSPAllowFlight(),
            new PSPArrowsInBody(),
            new PSPBedSpawnLocation(),
            new PSPCollidable(),
            new PSPCompassTarget(),
            new PSPEnderChest(),
            new PSPExhaustion(),
            new PSPExperienceLevel(),
            new PSPExperienceProgress(),
            new PSPFallDistance(),
            new PSPFireTicks(),
            new PSPFlying(),
            new PSPFlySpeed(),
            new PSPFoodLevel(),
            new PSPFreezeTicks(),
            new PSPGameMode(),
            new PSPGliding(),
            new PSPGravity(),
            new PSPHealth(),
            new PSPHealthScale(),
            new PSPHealthScaled(),
            new PSPHeldItemSlot(),
            new PSPInventoryArmor(),
            new PSPInventoryContents(),
            new PSPInvisible(),
            new PSPInvulnerable(),
            new PSPLastDeathLocation(),
            new PSPLocation(),
            new PSPOffHand(),
            new PSPPotionEffects(),
            new PSPRemainingAir(),
            new PSPSaturation(),
            new PSPStatistics(),
            new PSPSwimming(),
            new PSPTicksLived(),
            new PSPVelocity(),
            new PSPWalkSpeed()
    };

    public static void addToConfiguration(ConfigurationSection configSection, Player player) {
        for(PlayerStateProperty<?> property : properties)
            configSection.set(property.getConfigName(), property.getSerializedPropertyFromPlayer(player));
    }

    public static void restoreFromConfiguration(ConfigurationSection configSection, Player player) {
        for(PlayerStateProperty<?> property : properties) {
            Object value = property.getValueFromConfig(configSection);

            if(value != null) {
                @SuppressWarnings("unchecked")
                PlayerStateProperty<Object> typedProperty = (PlayerStateProperty<Object>) property;
                typedProperty.restoreProperty(player, value);
            }
        }
    }

    public static void restoreToDefault(Player player) {
        for(PlayerStateProperty<?> property : properties) {
            @SuppressWarnings("unchecked")
            PlayerStateProperty<Object> typedProperty = (PlayerStateProperty<Object>) property;
            typedProperty.restoreProperty(player, property.getDefaultSerializedProperty());
        }
    }
}
