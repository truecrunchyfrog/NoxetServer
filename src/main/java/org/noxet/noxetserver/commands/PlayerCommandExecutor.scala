package org.noxet.noxetserver.commands

import org.bukkit.command.{Command, CommandExecutor, CommandSender}
import org.bukkit.entity.Player
import org.noxet.noxetserver.messaging.ErrorMessage
import org.noxet.noxetserver.messaging.ErrorMessage.ErrorType.Common

trait PlayerCommandExecutor extends CommandExecutor:
  def onPlayerCommand(player: Player): Boolean =
    throw NotImplementedError("either onCommand method must be implemented.")

  def onPlayerCommand(player: Player, command: Command, s: String, strings: Array[String]): Boolean =
    onPlayerCommand(player)

  final override def onCommand(commandSender: CommandSender, command: Command, s: String, strings: Array[String]): Boolean =
    commandSender match
      case p: Player => onPlayerCommand(p, command, s, strings)
      case _ =>
        ErrorMessage(Common, "Only players can issue this command.")
          .send(commandSender)
        true