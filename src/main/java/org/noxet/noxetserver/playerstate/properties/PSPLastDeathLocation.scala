package org.noxet.noxetserver.playerstate.properties

import org.bukkit.Location
import org.bukkit.entity.Player
import org.noxet.noxetserver.playerstate.PlayerStateProperty

object PSPLastDeathLocation extends PlayerStateProperty[Location]:
  override def getConfigName: String = "last_death_location"

  override def getDefaultSerializedProperty: Location = null

  override def getSerializedPropertyFromPlayer(player: Player): Location =
    player.getLastDeathLocation

  override def restoreProperty(player: Player, deathLocation: Location): Unit =
    player.setLastDeathLocation(deathLocation)