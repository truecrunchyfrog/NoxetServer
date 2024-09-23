package org.noxet.noxetserver.playerstate.properties

import org.bukkit.entity.Player
import org.noxet.noxetserver.playerstate.PlayerStateProperty

object PSPAllowFlight extends PlayerStateProperty[Boolean]:
  override def getConfigName: String = "allow_flight"

  override def getDefaultSerializedProperty: Boolean = false

  override def getSerializedPropertyFromPlayer(player: Player): Boolean = player.getAllowFlight

  override def restoreProperty(player: Player, allow: Boolean): Unit = player.setAllowFlight(allow)
