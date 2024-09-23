package org.noxet.noxetserver.playerstate.properties

import org.bukkit.Location
import org.bukkit.entity.Player
import org.noxet.noxetserver.playerstate.PlayerStateProperty

object PSPBedSpawnLocation extends PlayerStateProperty[Location]:
  override def getConfigName: String = "bed_spawn_location"

  override def getDefaultSerializedProperty: Location = null

  override def getSerializedPropertyFromPlayer(player: Player): Location =
    player.getBedSpawnLocation

  override def restoreProperty(player: Player, spawnLocation: Location): Unit =
    player.setBedSpawnLocation(spawnLocation)