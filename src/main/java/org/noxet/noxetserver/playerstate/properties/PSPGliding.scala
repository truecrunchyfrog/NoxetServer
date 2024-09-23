package org.noxet.noxetserver.playerstate.properties

import org.bukkit.entity.Player
import org.noxet.noxetserver.playerstate.PlayerStateProperty

object PSPGliding extends PlayerStateProperty[Boolean]:
  override def getConfigName: String = "gliding"

  override def getDefaultSerializedProperty: Boolean = false

  override def getSerializedPropertyFromPlayer(player: Player): Boolean =
    player.isGliding

  override def restoreProperty(player: Player, gliding: Boolean): Unit =
    player.setGliding(gliding)