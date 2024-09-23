package org.noxet.noxetserver.playerstate.properties

import org.bukkit.entity.Player
import org.noxet.noxetserver.playerstate.PlayerStateProperty

object PSPInvulnerable extends PlayerStateProperty[Boolean]:
  override def getConfigName: String = "invulnerable"

  override def getDefaultSerializedProperty: Boolean = false

  override def getSerializedPropertyFromPlayer(player: Player): Boolean =
    player.isInvulnerable

  override def restoreProperty(player: Player, invulnerable: Boolean): Unit =
    player.setInvulnerable(invulnerable)