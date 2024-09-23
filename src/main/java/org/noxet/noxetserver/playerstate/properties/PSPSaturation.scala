package org.noxet.noxetserver.playerstate.properties

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.noxet.noxetserver.playerstate.PlayerStateProperty

object PSPSaturation extends PlayerStateProperty[Float]:
  override def getConfigName: String = "saturation"

  override def getDefaultSerializedProperty: Float = 20

  override def getSerializedPropertyFromPlayer(player: Player): Float =
    player.getSaturation

  override def restoreProperty(player: Player, saturation: Float): Unit =
    player.setSaturation(saturation)

  override def getValueFromConfig(config: ConfigurationSection): Float =
    config.getDouble(getConfigName).toFloat