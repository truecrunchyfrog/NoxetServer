package org.noxet.noxetserver.playerstate.properties

import org.bukkit.entity.Player
import org.noxet.noxetserver.playerstate.PlayerStateProperty

object PSPFreezeTicks extends PlayerStateProperty[Integer]:
  override def getConfigName: String = "freeze_ticks"

  override def getDefaultSerializedProperty: Integer = 0

  override def getSerializedPropertyFromPlayer(player: Player): Integer =
    player.getFreezeTicks

  override def restoreProperty(player: Player, ticks: Integer): Unit =
    player.setFreezeTicks(ticks)