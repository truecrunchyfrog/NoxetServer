package org.noxet.noxetserver.playerstate.properties

import org.bukkit.entity.Player
import org.noxet.noxetserver.playerstate.PlayerStateProperty

object PSPFireTicks extends PlayerStateProperty[Integer]:
  override def getConfigName: String = "fire_ticks"

  override def getDefaultSerializedProperty: Integer = 0

  override def getSerializedPropertyFromPlayer(player: Player): Integer =
    player.getFireTicks

  override def restoreProperty(player: Player, ticks: Integer): Unit =
    player.setFireTicks(ticks)
