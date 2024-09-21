package org.noxet.noxetserver.creepersweeper

import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.noxet.noxetserver.menus.ItemGenerator
import org.noxet.noxetserver.util.InventoryCoordinate

import scala.collection.immutable.{ArraySeq, HashMap}

class CreeperSweeperTile:
  var revealed = false
  var creeper = false
  var flagged = false

  def toItemStack(creeperNeighbors: Int): ItemStack =
    import ItemGenerator.generateItem as item
    if !revealed then
      // Covered tile.
      if !flagged then item(Material.GRASS_BLOCK, "§r")
      else item(Material.CREEPER_BANNER_PATTERN, "§c⚐")
    else if creeper then
      // Uncovered creeper tile.
      item(Material.CREEPER_HEAD, "§c§lCREEPER!")
    else if creeperNeighbors > 0 then
      // Uncovered safe tile (with creeper proximity number).
      item(CountHintColors.get(creeperNeighbors - 1), creeperNeighbors, "§r", None)
    else
      // Uncovered safe tile.
      item(Material.LIGHT_GRAY_STAINED_GLASS_PANE, "§r")

object CreeperSweeperTile:

  import Material.*

  private val CountHintColors = ArraySeq(
    LIGHT_BLUE_DYE, // 1
    GREEN_DYE, // 2
    RED_DYE, // 3
    BLUE_DYE, // 4
    ORANGE_DYE, // 5
    CYAN_DYE, // 6
    BLACK_DYE, // 7
    GRAY_DYE, // 8
    PURPLE_DYE // 9
  )