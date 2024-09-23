package org.noxet.noxetserver.playerstate.properties

import org.bukkit.entity.Player
import org.noxet.noxetserver.playerstate.PlayerStateProperty

object PSPSwimming extends PlayerStateProperty[Boolean]:
  override def getConfigName: String = "swimming"

  override def getDefaultSerializedProperty: Boolean = false

  override def getSerializedPropertyFromPlayer(player: Player): Boolean =
    player.isSwimming

  override def restoreProperty(player: Player, swimming: Boolean): Unit =
    player.setSwimming(swimming)