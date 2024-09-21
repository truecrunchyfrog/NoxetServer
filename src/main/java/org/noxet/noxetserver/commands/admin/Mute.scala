package org.noxet.noxetserver.commands.admin

import org.bukkit.command.{Command, CommandSender}
import org.bukkit.entity.Player
import org.noxet.noxetserver.NoxetServer
import org.noxet.noxetserver.commands.RegisteredCommand
import org.noxet.noxetserver.messaging.{ErrorMessage, SuccessMessage}
import org.noxet.noxetserver.playerdata.PlayerDataManager

object Mute extends OperatorTabExecutor, RegisteredCommand("mute", this):
  override def onOperatorCommand(commandSender: CommandSender, command: Command, s: String, strings: Array[String]): Boolean =
    strings match
      case Array(playerName) =>
        Option(NoxetServer.getPlugin.getServer.getPlayer(playerName)) match
          case Some(playerToMute) =>
            val playerDataManager = PlayerDataManager(playerToMute)

            if playerDataManager.get(PlayerDataManager.Attribute.Muted).asInstanceOf[Boolean] then
              ErrorMessage(Common, "That player is already muted.").send(commandSender)
              return true

            playerDataManager.set(PlayerDataManager.Attribute.Muted, true).save()
            SuccessMessage(s"${playerToMute.getName} has been muted and can no longer chat.")
              .addButton("Unmute", ChatColor.RED, "Undo the mute", s"unmute ${playerToMute.getName}").send(commandSender)

            true
          case None =>
            ErrorMessage(Argument, "That is not an online player.").send(commandSender)
            true
      case _ =>
        ErrorMessage(Argument, "Missing player to mute.").send(commandSender)
        false


  override def onOperatorTabComplete(commandSender: CommandSender, command: Command, s: String, strings: Array[String]): List[String] =
    strings match
      case Array(identifier) =>
        Option(NoxetServer.getPlugin.getServer.getPlayer(identifier))
          .map(_.getName)
          .toList
      case _ => List()