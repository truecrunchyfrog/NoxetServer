package org.noxet.noxetserver.commands.games.creepersweeper

import org.bukkit.command.Command
import org.bukkit.entity.Player
import org.noxet.noxetserver.commands.{PlayerCommandExecutor, RegisteredCommand}
import org.noxet.noxetserver.menus.inventory.ConfirmationMenu
import org.noxet.noxetserver.messaging.{ErrorMessage, NoteMessage, SuccessMessage}
import org.noxet.noxetserver.playerdata.PlayerDataManager

object ClearCreeperSweeperStats extends PlayerCommandExecutor, RegisteredCommand("clear-creeper-sweeper-stats", this):
  override def onPlayerCommand(player: Player): Boolean =
    ConfirmationMenu("Clear all Creeper Sweeper stats?", () =>
      val playerDataManager = PlayerDataManager(player)

      playerDataManager.remove(PlayerDataManager.Attribute.CreeperSweeperWins)
      playerDataManager.remove(PlayerDataManager.Attribute.CreeperSweeperLosses)
      playerDataManager.remove(PlayerDataManager.Attribute.CreeperSweeperTotalWinPlaytime)

      playerDataManager.save()

      SuccessMessage("Your Creeper Sweeper stats have been deleted.").send(player)
      ,
      NoteMessage("Phew! Your Creeper Sweeper game stats remain.").send(player)
    ).openInventory(player)
    true