package org.noxet.noxetserver.menus.inventory

import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable
import org.noxet.noxetserver.NoxetServer
import org.noxet.noxetserver.creepersweeper.CreeperSweeperGame
import org.noxet.noxetserver.creepersweeper.CreeperSweeperTile
import org.noxet.noxetserver.menus.ItemGenerator
import org.noxet.noxetserver.menus.inventory.CreeperSweeperGameMenu.SurpriseFill
import org.noxet.noxetserver.messaging.Message
import org.noxet.noxetserver.playerdata.PlayerDataManager
import org.noxet.noxetserver.util.{FancyTimeConverter, InventoryCoordinate, QuickRunnable}

import java.text.DecimalFormat
import java.util.Arrays
import java.util.Iterator
import java.util.List
import java.util.Map

class CreeperSweeperGameMenu(height: Int, creepers: Int)
  extends InventoryMenu(
      height,
      "Creeper Sweeper (9x" + height + ", " + creepers + " creepers)",
      false):
    private val game = CreeperSweeperGame(9, height, creepers)

    override protected def updateInventory(): Unit =
        game.tiles.foreach(setSlotItem(_._2.toItemStack, _._1))

    override protected def onSlotClick(player: Player, coordinate: InventoryCoordinate, clickType: ClickType): Boolean =
        val clickedTile = game.tiles.get(coordinate)

        if clickedTile.isEmpty then
            return false

        if game.hasEnded then
            CreeperSweeperGameMenu(height, creepers).openInventory(player)
            return true

        clickType match
            case ClickType.LEFT => game.revealTile(coordinate)
            case ClickType.RIGHT => clickedTile.flagged ^= true

        var surpriseFill: Option[SurpriseFill] = None
        
        if game.hasEnded then
            val playerDataManager = PlayerDataManager(player)

            if game.didWin then
                player.playSound(player, Sound.ENTITY_CREEPER_DEATH, 1, 0.5f)

                Message(s"§eYou beat Creeper Sweeper in §c${FancyTimeConverter.deltaSecondsToFancyTime((game.getGameDuration / 1000).toInt)}§e.").send(player)

                playerDataManager.incrementInt(PlayerDataManager.Attribute.CreeperSweeperWins)
                playerDataManager.addLong(PlayerDataManager.Attribute.CreeperSweeperTotalWinPlaytime, game.getGameDuration / 1000)

                surpriseFill = Some(SurpriseFill.Win)
            else
                player.playSound(player, Sound.ENTITY_CREEPER_PRIMED, 1, 1)
                QuickRunnable(player.playSound(player, Sound.ENTITY_GENERIC_EXPLODE, 1, 1))
                  .runTaskLater(NoxetServer.getPlugin, 20)

                Message("§c§lTss...! You revealed a creeper.").send(player)

                playerDataManager.incrementInt(PlayerDataManager.Attribute.CreeperSweeperLosses)

                surpriseFill = Some(SurpriseFill.Lose)

            val wins = playerDataManager.get(PlayerDataManager.Attribute.CreeperSweeperWins).toInt
            val losses = playerDataManager.get(PlayerDataManager.Attribute.CreeperSweeperLosses).toInt
          
            Message.add(
                s"W/L: §e${DecimalFormat("###.###").format(wins / Math.max(losses, 1).toDouble)}§7 ($wins wins, $losses losses)",
                "The higher W/L, the better. To clear these stats, type /clear-creeper-sweeper-stats."
            ).send(player)

            val totalWinPlaytime = playerDataManager.get(
              PlayerDataManager.Attribute.CreeperSweeperTotalWinPlaytime).toLong

            if wins > 0 then
                Message("Average time (wins): §e" + FancyTimeConverter.deltaSecondsToFancyTime((totalWinPlaytime / wins).toInt)).send(player)

            playerDataManager.save()

        surpriseFill match
          case Some(fill) =>
            fill.fillInventory(getInventory)
            QuickRunnable(updateInventory()).runTaskLater(NoxetServer.getPlugin, 20)
          case None => updateInventory()

        false

object CreeperSweeperGameMenu:
    enum SurpriseFill(itemStack: ItemStack):
        case Win extends SurpriseFill(ItemGenerator.generateItem(Material.EMERALD, "§a§lFinished!"))
        case Lose extends SurpriseFill(ItemGenerator.generateItem(Material.TNT, "§c§lAwww..."))

        def fillInventory(inventory: Inventory): Unit =
            (0 until inventory.getSize).foreach(inventory.setItem(_, itemStack))