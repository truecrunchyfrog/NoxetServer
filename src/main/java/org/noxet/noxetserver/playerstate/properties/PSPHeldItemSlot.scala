package org.noxet.noxetserver.playerstate.properties

import org.bukkit.entity.Player
import org.noxet.noxetserver.playerstate.PlayerStateProperty

object PSPHeldItemSlot extends PlayerStateProperty[Integer]:
  override def getConfigName: String = "held_item_slot"

  override def getDefaultSerializedProperty: Integer = 0

  override def getSerializedPropertyFromPlayer(player: Player): Integer =
    player.getInventory.getHeldItemSlot

  override def restoreProperty(player: Player, slot: Integer): Unit =
    player.getInventory.setHeldItemSlot(slot)