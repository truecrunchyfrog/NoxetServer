package org.noxet.noxetserver.playerstate.properties

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.noxet.noxetserver.playerstate.PlayerStateProperty

object PSPAbsorptionAmount extends PlayerStateProperty[Double]:
  override def getConfigName: String = "absorption_amount"

  override def getDefaultSerializedProperty: Double = 0D

  override def getSerializedPropertyFromPlayer(player: Player): Double =
    player.getAbsorptionAmount

  override def restoreProperty(player: Player, absorption: Double): Unit =
    player.setAbsorptionAmount(absorption)

  override def getValueFromConfig(config: ConfigurationSection): Double =
    config.getDouble(getConfigName)