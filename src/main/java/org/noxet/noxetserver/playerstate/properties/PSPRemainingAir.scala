package org.noxet.noxetserver.playerstate.properties

import org.bukkit.entity.Player
import org.noxet.noxetserver.playerstate.PlayerStateProperty

object PSPRemainingAir extends PlayerStateProperty[Integer]:
  override def getConfigName: String = "remaining_air"

  override def getDefaultSerializedProperty: Integer = 360

  override def getSerializedPropertyFromPlayer(player: Player): Integer =
    player.getRemainingAir

  override def restoreProperty(player: Player, ticks: Integer): Unit =
    player.setRemainingAir(ticks)