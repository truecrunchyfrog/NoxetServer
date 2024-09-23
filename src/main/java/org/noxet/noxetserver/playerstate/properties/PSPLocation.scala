package org.noxet.noxetserver.playerstate.properties

import org.bukkit.Location
import org.bukkit.entity.Player
import org.noxet.noxetserver.playerstate.PlayerStateProperty

object PSPLocation extends PlayerStateProperty[Location]:
  override def getConfigName: String = "location"

  override def getDefaultSerializedProperty: Location = null

  override def getSerializedPropertyFromPlayer(player: Player): Location =
    player.getLocation

  override def restoreProperty(player: Player, location: Location): Unit =
    if location != null then
      player.teleport(location)