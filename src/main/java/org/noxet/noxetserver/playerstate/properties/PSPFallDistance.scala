package org.noxet.noxetserver.playerstate.properties

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.noxet.noxetserver.playerstate.PlayerStateProperty

object PSPFallDistance extends PlayerStateProperty[Float]:
  override def getConfigName: String = "fall_distance"

  override def getDefaultSerializedProperty: Float = 0

  override def getSerializedPropertyFromPlayer(player: Player): Float =
    player.getFallDistance

  override def restoreProperty(player: Player, distance: Float): Unit =
    player.setFallDistance(distance)

  override def getValueFromConfig(config: ConfigurationSection): Float =
    config.getDouble(getConfigName).toFloat