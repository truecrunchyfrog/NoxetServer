package org.noxet.noxetserver.playerstate.properties

import org.bukkit.entity.Player
import org.noxet.noxetserver.playerstate.PlayerStateProperty

object PSPTicksLived extends PlayerStateProperty[Int]:
  override def getConfigName: String = "ticks_lived"

  override def getDefaultSerializedProperty: Int = 1

  override def getSerializedPropertyFromPlayer(player: Player): Int =
    player.getTicksLived

  override def restoreProperty(player: Player, ticks: Int): Unit =
    player.setTicksLived(ticks)