package org.noxet.noxetserver.playerstate.properties

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.noxet.noxetserver.playerstate.PlayerStateProperty

object PSPWalkSpeed extends PlayerStateProperty[Float]:
  override def getConfigName: String = "walk_speed"

  override def getDefaultSerializedProperty: Float = 0.2

  override def getSerializedPropertyFromPlayer(player: Player): Float =
    player.getWalkSpeed

  override def restoreProperty(player: Player, speed: Float): Unit =
    player.setWalkSpeed(speed)

  override def getValueFromConfig(config: ConfigurationSection): Float =
    config.getDouble(getConfigName).toFloat