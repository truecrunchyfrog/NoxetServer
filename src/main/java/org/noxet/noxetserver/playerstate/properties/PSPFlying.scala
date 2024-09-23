package org.noxet.noxetserver.playerstate.properties

import org.bukkit.entity.Player
import org.noxet.noxetserver.playerstate.PlayerStateProperty

object PSPFlying extends PlayerStateProperty[Boolean]:
  override def getConfigName: String = "flying"

  override def getDefaultSerializedProperty: Boolean = false

  override def getSerializedPropertyFromPlayer(player: Player): Boolean =
    player.isFlying

  override def restoreProperty(player: Player, flying: Boolean): Unit =
    player.setFlying(flying)