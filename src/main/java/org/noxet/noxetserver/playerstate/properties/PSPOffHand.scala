package org.noxet.noxetserver.playerstate.properties

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.noxet.noxetserver.playerstate.PlayerStateProperty

object PSPOffHand extends PlayerStateProperty[ItemStack]:
  override def getConfigName: String = "off_hand"

  override def getDefaultSerializedProperty: ItemStack =
    ItemStack(Material.AIR)

  override def getSerializedPropertyFromPlayer(player: Player): ItemStack =
    player.getInventory.getItemInOffHand

  override def restoreProperty(player: Player, offHand: ItemStack): Unit =
    player.getInventory.setItemInOffHand(offHand)