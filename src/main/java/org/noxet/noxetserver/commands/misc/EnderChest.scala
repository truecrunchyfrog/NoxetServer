package org.noxet.noxetserver.commands.misc

import org.bukkit.command.{Command, CommandExecutor, CommandSender}
import org.bukkit.entity.Player
import org.bukkit.inventory.InventoryView
import org.noxet.noxetserver.NoxetServer
import org.noxet.noxetserver.commands.{PlayerCommandExecutor, RegisteredCommand}
import org.noxet.noxetserver.messaging.ErrorMessage
import org.noxet.noxetserver.messaging.ErrorMessage.ErrorType.*
import org.noxet.noxetserver.realm.RealmManager

object EnderChest extends PlayerCommandExecutor, RegisteredCommand("enderchest", this):
  override def onPlayerCommand(player: Player, command: Command, s: String, strings: Array[String]): Boolean =
    RealmManager.getCurrentRealm(player) match
      case Some(realm) if realm.allowTeleportationMethods =>
        val chestOwner = strings match
          case Array(playerName) =>
            if !player.isOp then
              ErrorMessage(Permission, "You are not allowed to open other players' ender chests.").send(player)
              return true

            Option(NoxetServer.getPlugin.getServer.getPlayer(playerName)) match
              case Some(player) => player
              case None =>
                ErrorMessage(Permission, "Invalid player.").send(player)
                return true
          case Array() => player
          case _ =>
            ErrorMessage(Permission, "Invalid arguments.").send(player)
            return true

        Option(player.openInventory(chestOwner.getEnderChest)) match
          case Some(view) if chestOwner != player =>
            inventoryView.setTitle(s"${peekOn.getName}'s ${inventoryView.getTitle}")
          case None =>
            ErrorMessage(Common, "Failed to open inventory.").send(player)
            return true
          case _ => () // Normal operation
        true
      case _ =>
        ErrorMessage(Permission, "You may not use /enderchest here.").send(player)
        true