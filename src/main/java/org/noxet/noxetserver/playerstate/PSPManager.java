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
            new PSPExperienceLevel(),
            new PSPExperienceProgress(),
            new PSPFallDistance(),
            new PSPFireTicks(),
            new PSPFlying(),
            new PSPFlySpeed(),
            new PSPFoodLevel(),
            new PSPFreezeTicks(),
            new PSPGameMode(),
            new PSPGravity(),
            new PSPHealth(),
            new PSPHealthScale(),
            new PSPHealthScaled(),
            new PSPInvisible(),
            new PSPInvulnerable(),
            new PSPLastDeathLocation(),
            new PSPLocation(),
            new PSPPlayerInventory(),
            new PSPPotionEffects(),
            new PSPRemainingAir(),
            new PSPSaturation(),
            new PSPStatistics(),
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
            Class<?> type = property.getTypeClass();
            Object value = configSection.getObject(property.getConfigName(), property.getTypeClass());

            if(type != null && value != null) {
                @SuppressWarnings("unchecked")
                PlayerStateProperty<Object> typedProperty = (PlayerStateProperty<Object>) property;
                typedProperty.restoreProperty(player, value);
            }
        }
    }

    public static void restoreToDefault(Player player) {
        for(PlayerStateProperty<?> property : properties) {
            Class<?> type = property.getTypeClass();

            if(type != null) {
                @SuppressWarnings("unchecked")
                PlayerStateProperty<Object> typedProperty = (PlayerStateProperty<Object>) property;
                typedProperty.restoreProperty(player, typedProperty.getDefaultSerializedProperty());
            }
        }
    }
}
