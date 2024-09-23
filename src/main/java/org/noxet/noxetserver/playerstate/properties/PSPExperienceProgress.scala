package org.noxet.noxetserver.playerstate.properties

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.noxet.noxetserver.playerstate.PlayerStateProperty

object PSPExperienceProgress extends PlayerStateProperty[Float]:
  override def getConfigName: String = "experience_progress"

  override def getDefaultSerializedProperty: Float = 0F

  override def getSerializedPropertyFromPlayer(player: Player): Float =
    player.getExp

  override def restoreProperty(player: Player, progress: Float): Unit =
    player.setExp(progress)

  override def getValueFromConfig(config: ConfigurationSection): Float =
    config.getDouble(getConfigName).toFloat