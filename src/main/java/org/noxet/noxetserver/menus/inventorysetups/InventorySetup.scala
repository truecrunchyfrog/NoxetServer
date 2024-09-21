package org.noxet.noxetserver.menus.inventorysetups

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.Inventory

trait InventorySetup:
  protected val inventory: Inventory = Bukkit.createInventory(null, InventoryType.PLAYER)

  protected def populateInventory(): Unit

  def applyToPlayer(player: Player): Unit =
    populateInventory()
    player.getInventory.setContents(inventory.getContents)