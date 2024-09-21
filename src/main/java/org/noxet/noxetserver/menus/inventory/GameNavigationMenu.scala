package org.noxet.noxetserver.menus.inventory

import org.bukkit.{ChatColor, Material}
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack
import org.noxet.noxetserver.menus.ItemGenerator.generateItem
import org.noxet.noxetserver.menus.inventory.GameNavigationMenu.{AnarchyIsland, CreeperSweeper, Smp, WorldEater}
import org.noxet.noxetserver.minigames.{GameDefinition, MiniGameManager}
import org.noxet.noxetserver.realm.RealmManager
import org.noxet.noxetserver.util.{InventoryCoordinate, TextBeautifier}

class GameNavigationMenu extends InventoryMenu(
  3,
  "⭐ " + TextBeautifier.beautify("Noxet - Choose a game"),
  false):
  override protected def updateInventory(): Unit =
    setSlotItem(generateGameModeItem(
      "Anarchy Island",
      ChatColor.RED,
      List("Insane vanilla experience.", "No rules. Try to live", "in a destructive world."),
      RealmManager.Realm.Anarchy.getPlayerCount,
      Material.FLINT_AND_STEEL
    ), AnarchyIsland)

    setSlotItem(generateGameModeItem(
      "WorldEater",
      ChatColor.DARK_GREEN,
      List("Hide and seek in one chunk.", "Hiders win if survived for", "30 minutes."),
      MiniGameManager.countPlayersInGame(GameDefinition.WorldEater),
      Material.SPRUCE_LOG
    ), WorldEater)

    setSlotItem(generateGameModeItem(
      "Smp",
      ChatColor.BLUE,
      List("The (somewhat) vanilla", "experience."),
      RealmManager.Realm.SMP.getPlayerCount,
      Material.GRASS_BLOCK
    ), Smp)

    setSlotItem(generateGameModeItem(
      "Creeper Sweeper",
      ChatColor.YELLOW,
      List("Minesweeper clone, enjoy", "anywhere!"),
      0,
      Material.CREEPER_HEAD
    ), CreeperSweeper)


  private def generateGameModeItem(name: String, nameColor: ChatColor, description: List[String], players: Int, material: Material): ItemStack =
    generateItem(
      material,
      nameColor + TextBeautifier.beautify(name, false),
      Some(s"§9$players in game." :+ description.map("§7§o" + _))
    )

  override protected def onSlotClick(player: Player, coordinate: InventoryCoordinate, clickType: ClickType): Boolean =
    coordinate match
      case AnarchyIsland =>
        RealmManager.migrateToRealm(player, RealmManager.Realm.ANARCHY)
      case WorldEater =>
        player.performCommand("game play world-eater")
      case Smp =>
        RealmManager.migrateToRealm(player, RealmManager.Realm.SMP)
      case CreeperSweeper =>
        player.performCommand("creeper-sweeper")
      case _ => return false
    true

object GameNavigationMenu:
  val AnarchyIsland = InventoryCoordinate(2, 1)
  val WorldEater = InventoryCoordinate(4, 1)
  val Smp = InventoryCoordinate(6, 1)
  val CreeperSweeper = InventoryCoordinate(8, 2)