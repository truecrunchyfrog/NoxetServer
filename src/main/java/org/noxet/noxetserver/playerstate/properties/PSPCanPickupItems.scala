package org.noxet.noxetserver.playerstate.properties

import org.bukkit.entity.Player
import org.noxet.noxetserver.playerstate.PlayerStateProperty

object PSPCanPickupItems extends PlayerStateProperty[Boolean]:
  override def getConfigName: String = "can_pickup_items"

  override def getDefaultSerializedProperty: Boolean = true

  override def getSerializedPropertyFromPlayer(player: Player): Boolean =
    player.getCanPickupItems

  override def restoreProperty(player: Player, canPickupItems: Boolean): Unit =
    player.setCanPickupItems(canPickupItems)