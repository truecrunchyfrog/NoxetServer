package org.noxet.noxetserver.playerstate.properties

import org.bukkit.Location
import org.bukkit.entity.Player
import org.noxet.noxetserver.NoxetServer
import org.noxet.noxetserver.playerstate.PlayerStateProperty

object PSPCompassTarget extends PlayerStateProperty[Location]:
  override def getConfigName: String = "compass_target"

  override def getDefaultSerializedProperty: Location =
    NoxetServer.ServerWorld.HUB.getWorld.getSpawnLocation

  override def getSerializedPropertyFromPlayer(player: Player): Location =
    player.getCompassTarget

  override def restoreProperty(player: Player, target: Location): Unit =
    player.setCompassTarget(target)