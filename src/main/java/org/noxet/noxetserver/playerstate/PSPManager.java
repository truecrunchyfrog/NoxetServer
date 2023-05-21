package org.noxet.noxetserver.playerstate;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.noxet.noxetserver.playerstate.properties.*;

import java.lang.reflect.InvocationTargetException;

public class PSPManager {
    private static final Class<?>[] propertyClassList = {
            PSPAbsorptionAmount.class,
            PSPAdvancementCriteria.class,
            PSPAllowFlight.class,
            PSPArrowsInBody.class,
            PSPBedSpawnLocation.class,
            PSPCollidable.class,
            PSPCompassTarget.class,
            PSPEnderChest.class,
            PSPExperienceLevel.class,
            PSPExperienceProgress.class,
            PSPFallDistance.class,
            PSPFireTicks.class,
            PSPFlying.class,
            PSPFlySpeed.class,
            PSPFoodLevel.class,
            PSPGameMode.class,
            PSPGravity.class,
            PSPHealth.class,
            PSPHealthScale.class,
            PSPHealthScaled.class,
            PSPInvisible.class,
            PSPInvulnerable.class,
            PSPLastDeathLocation.class,
            PSPLocation.class,
            PSPPlayerInventory.class,
            PSPPotionEffects.class,
            PSPRemainingAir.class,
            PSPSaturation.class,
            PSPStatistics.class,
            PSPVelocity.class,
            PSPWalkSpeed.class
    };

    @SuppressWarnings("unchecked")
    public static void addToConfiguration(ConfigurationSection configSection, Player player) {
        for(Class<?> propertyClass : propertyClassList) {
            Class<? extends PlayerStateProperty> playerStatePropertyClass = (Class<? extends PlayerStateProperty>) propertyClass;
            try {
                playerStatePropertyClass.getDeclaredConstructor().newInstance().addToConfiguration(configSection, player);
            } catch (InstantiationException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static void restoreFromConfiguration(ConfigurationSection configSection, Player player) {
        for(Class<?> propertyClass : propertyClassList) {
            Class<? extends PlayerStateProperty> playerStatePropertyClass = (Class<? extends PlayerStateProperty>) propertyClass;
            try {
                playerStatePropertyClass.getDeclaredConstructor().newInstance().restoreProperty(configSection, player);
            } catch (InstantiationException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
