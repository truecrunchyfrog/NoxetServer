package org.noxet.noxetserver.playerstate.properties

import org.bukkit.entity.Player
import org.noxet.noxetserver.playerstate.PlayerStateProperty

object PSPCollidable extends PlayerStateProperty[Boolean]:
  override def getConfigName: String = "collidable"

  override def getDefaultSerializedProperty: Boolean = true

  override def getSerializedPropertyFromPlayer(player: Player): Boolean =
    player.isCollidable

  override def restoreProperty(player: Player, collidable: Boolean): Unit =
    player.setCollidable(collidable)