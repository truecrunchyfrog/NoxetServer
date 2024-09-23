package org.noxet.noxetserver.playerstate.properties

import org.bukkit.entity.Player
import org.noxet.noxetserver.playerstate.PlayerStateProperty

object PSPHealthScaled extends PlayerStateProperty[Boolean]:
  override def getConfigName: String = "health_scaled"

  override def getDefaultSerializedProperty: Boolean = false

  override def getSerializedPropertyFromPlayer(player: Player): Boolean =
    player.isHealthScaled

  override def restoreProperty(player: Player, scaled: Boolean): Unit =
    player.setHealthScaled(scaled)