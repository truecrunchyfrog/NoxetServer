package org.noxet.noxetserver.playerstate.properties

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.noxet.noxetserver.playerstate.PlayerStateProperty

object PSPFlySpeed extends PlayerStateProperty[Float]:
  override def getConfigName: String = "fly_speed"

  override def getDefaultSerializedProperty: Float = 0.1

  override def getSerializedPropertyFromPlayer(player: Player): Float =
    player.getFlySpeed

  override def restoreProperty(player: Player, speed: Float): Unit =
    player.setFlySpeed(speed)

  override def getValueFromConfig(config: ConfigurationSection): Float =
    config.getDouble(getConfigName).toFloat