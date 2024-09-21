package org.noxet.noxetserver.commands.games.creepersweeper

import org.bukkit.command.Command
import org.bukkit.entity.Player
import org.noxet.noxetserver.commands.{PlayerCommandExecutor, RegisteredCommand}
import org.noxet.noxetserver.menus.inventory.CreeperSweeperGameMenu
import org.noxet.noxetserver.messaging.ErrorMessage

object CreeperSweeper extends PlayerCommandExecutor, RegisteredCommand("creeper-sweeper", this):
  override def onPlayerCommand(player: Player): Boolean =
    CreeperSweeperGameMenu(6, 8).openInventory(player)
    true