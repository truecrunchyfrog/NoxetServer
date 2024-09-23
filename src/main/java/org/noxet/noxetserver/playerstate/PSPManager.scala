package org.noxet.noxetserver.playerstate

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.noxet.noxetserver.playerstate.properties.*

object PSPManager:
  private val properties: List[PlayerStateProperty[Any]] = List(
    PSPAbsorptionAmount,
    PSPAdvancementCriteria,
    PSPAllowFlight,
    PSPArrowsInBody,
    PSPBedSpawnLocation,
    PSPCanPickupItems,
    PSPCollidable,
    PSPCompassTarget,
    PSPEnderChest,
    PSPExhaustion,
    PSPExperienceLevel,
    PSPExperienceProgress,
    PSPFallDistance,
    PSPFireTicks,
    PSPFlying,
    PSPFlySpeed,
    PSPFoodLevel,
    PSPFreezeTicks,
    PSPGameMode,
    PSPGliding,
    PSPGravity,
    PSPHealth,
    PSPHealthScale,
    PSPHealthScaled,
    PSPHeldItemSlot,
    PSPInventoryArmor,
    PSPInventoryContents,
    PSPInvisible,
    PSPInvulnerable,
    PSPLastDeathLocation,
    PSPLocation,
    PSPPlayerTime,
    PSPOffHand,
    PSPPotionEffects,
    PSPRemainingAir,
    PSPSaturation,
    PSPScoreboard,
    PSPStatistics,
    PSPSwimming,
    PSPTicksLived,
    PSPVelocity,
    PSPWalkSpeed
  )

  def addToConfiguration(configSection: ConfigurationSection, player: Player): Unit =
    properties.foreach(p =>
      configSection.set(p.getConfigName,
        p.getSerializedPropertyFromPlayer(player)))

  def restoreFromConfiguration(configSection: ConfigurationSection, player: Player): Unit =
    properties
      .flatMap(p => p.getValueFromConfig(configSection).map(v => p -> v))
      .foreach((p, v) => p.restoreProperty(player, v))

  def restoreToDefault(player: Player): Unit =
    properties.foreach(prop =>
      prop.restoreProperty(player, prop.getDefaultSerializedProperty))