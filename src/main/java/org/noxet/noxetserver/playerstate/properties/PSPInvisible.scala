package org.noxet.noxetserver.playerstate.properties

import org.bukkit.entity.Player
import org.noxet.noxetserver.playerstate.PlayerStateProperty

object PSPInvisible extends PlayerStateProperty[Boolean]:
  override def getConfigName: String = "invisible"

  override def getDefaultSerializedProperty: Boolean = false

  override def getSerializedPropertyFromPlayer(player: Player): Boolean =
    player.isInvisible

  override def restoreProperty(player: Player, invisible: Boolean): Unit =
    player.setInvisible(invisible)