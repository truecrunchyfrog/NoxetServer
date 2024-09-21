package org.noxet.noxetserver.commands.admin

import org.bukkit.command.{Command, CommandSender, TabExecutor}
import org.bukkit.entity.Player
import org.noxet.noxetserver.NoxetServer
import org.noxet.noxetserver.commands.RegisteredCommand
import org.noxet.noxetserver.messaging.ErrorMessage.ErrorType.*
import org.noxet.noxetserver.messaging.{ErrorMessage, SuccessMessage}
import org.noxet.noxetserver.playerdata.PlayerDataManager

object Unmute extends OperatorTabExecutor, RegisteredCommand("unmute", this):
  override def onOperatorCommand(commandSender: CommandSender, command: Command, s: String, strings: Array[String]): Boolean =
    strings match
      case Array(playerName) =>
        Option(NoxetServer.getPlugin.getServer.getPlayer(playerName)) match
          case Some(playerToUnmute) =>
            val playerDataManager = PlayerDataManager(playerToUnmute)

            if !playerDataManager.get(PlayerDataManager.Attribute.Muted).toBoolean then
              ErrorMessage(Common, "That player is not muted.").send(commandSender)
              return true

            playerDataManager.set(PlayerDataManager.Attribute.Muted, false).save()
            SuccessMessage(
              s"${playerToUnmute.getName} has been unmuted and can now chat again.")
              .addButton("Mute", ChatColor.RED, "Redo the mute", s"mute ${playerToUnmute.getName}").send(commandSender)

            true
          case None =>
            ErrorMessage(Argument, "That is not an online player.")
              .send(commandSender)
            true
      case _ =>
        ErrorMessage(Argument, "Missing argument: player to unmute.")
          .send(commandSender)
        false

  override def onOperatorTabComplete(commandSender: CommandSender, command: Command, s: String, strings: Array[String]): List[String] =
    strings match
      case Array(playerName) =>
        Option(NoxetServer.getPlugin.getServer.getPlayer(playerName))
          .map(_.getName)
          .toList
      case _ => List()