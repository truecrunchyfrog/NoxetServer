package org.noxet.noxetserver.playerstate.properties

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.noxet.noxetserver.playerstate.PlayerStateProperty

object PSPHealthScale extends PlayerStateProperty[Double]:
  override def getConfigName: String = "health_scale"

  override def getDefaultSerializedProperty: Double = 20

  override def getSerializedPropertyFromPlayer(player: Player): Double = player.getHealthScale

  override def restoreProperty(player: Player, scale: Double): Unit =
    player.setHealthScale(scale)

  override def getValueFromConfig(config: ConfigurationSection): Double =
    config.getDouble(getConfigName)