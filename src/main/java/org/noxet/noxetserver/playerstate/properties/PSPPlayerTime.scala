package org.noxet.noxetserver.playerstate.properties

import org.bukkit.entity.Player
import org.noxet.noxetserver.playerstate.PlayerStateProperty

object PSPPlayerTime extends PlayerStateProperty[Long]:
  override def getConfigName: String = "player_time"

  override def getDefaultSerializedProperty: Long =
    null // This value shouldn't be saved.

  override def getSerializedPropertyFromPlayer(player: Player): Long =
    null // This value shouldn't be saved.

  override def restoreProperty(player: Player, value: Long): Unit =
    player.resetPlayerTime() // Just reset.