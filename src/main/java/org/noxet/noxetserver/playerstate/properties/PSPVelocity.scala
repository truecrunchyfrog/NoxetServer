package org.noxet.noxetserver.playerstate.properties

import org.bukkit.entity.Player
import org.bukkit.util.Vector
import org.noxet.noxetserver.playerstate.PlayerStateProperty

object PSPVelocity extends PlayerStateProperty[Vector]:
  override def getConfigName: String = "velocity"

  override def getDefaultSerializedProperty: Vector = Vector()

  override def getSerializedPropertyFromPlayer(player: Player): Vector =
    player.getVelocity

  override def restoreProperty(player: Player, velocity: Vector): Unit =
    player.setVelocity(velocity)