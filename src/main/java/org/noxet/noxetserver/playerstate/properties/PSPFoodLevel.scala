package org.noxet.noxetserver.playerstate.properties

import org.bukkit.entity.Player
import org.noxet.noxetserver.playerstate.PlayerStateProperty

object PSPFoodLevel extends PlayerStateProperty[Integer]:
  override def getConfigName: String = "food_level"

  override def getDefaultSerializedProperty: Integer = 20

  override def getSerializedPropertyFromPlayer(player: Player): Integer =
    player.getFoodLevel

  override def restoreProperty(player: Player, level: Integer): Unit =
    player.setFoodLevel(level)