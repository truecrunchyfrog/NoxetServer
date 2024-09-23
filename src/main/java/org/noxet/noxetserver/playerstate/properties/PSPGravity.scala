package org.noxet.noxetserver.playerstate.properties

import org.bukkit.entity.Player
import org.noxet.noxetserver.playerstate.PlayerStateProperty

object PSPGravity extends PlayerStateProperty[Boolean]:
  override def getConfigName: String = "gravity"

  override def getDefaultSerializedProperty: Boolean = true

  override def getSerializedPropertyFromPlayer(player: Player): Boolean = player.hasGravity

  override def restoreProperty(player: Player, gravity: Boolean): Unit =
    player.setGravity(gravity)