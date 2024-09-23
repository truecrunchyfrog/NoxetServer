package org.noxet.noxetserver.playerstate.properties

import org.bukkit.entity.Player
import org.noxet.noxetserver.playerstate.PlayerStateProperty

object PSPExperienceLevel extends PlayerStateProperty[Integer]:
  override def getConfigName: String = "experience_level"

  override def getDefaultSerializedProperty: Integer = 0

  override def getSerializedPropertyFromPlayer(player: Player): Integer =
    player.getLevel

  override def restoreProperty(player: Player, level: Integer): Unit =
    player.setLevel(level)