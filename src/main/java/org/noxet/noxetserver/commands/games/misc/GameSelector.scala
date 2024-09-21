package org.noxet.noxetserver.commands.games.misc

import org.bukkit.command.Command
import org.bukkit.entity.Player
import org.noxet.noxetserver.commands.{PlayerCommandExecutor, RegisteredCommand}
import org.noxet.noxetserver.menus.inventory.GameNavigationMenu

object GameSelector extends PlayerCommandExecutor, RegisteredCommand("games", this):
  override def onPlayerCommand(player: Player): Boolean =
    GameNavigationMenu().openInventory(player)
    true