package org.noxet.noxetserver.playerstate.properties

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.noxet.noxetserver.playerstate.PlayerStateProperty

object PSPExhaustion extends PlayerStateProperty[Float]:
  override def getConfigName: String = "exhaustion"

  override def getDefaultSerializedProperty: Float = 0F

  override def getSerializedPropertyFromPlayer(player: Player): Float =
    player.getExhaustion

  override def restoreProperty(player: Player, exhaustion: Float): Unit = player.setExhaustion(exhaustion)

  override def getValueFromConfig(config: ConfigurationSection): Float =
    config.getDouble(getConfigName).floatValue