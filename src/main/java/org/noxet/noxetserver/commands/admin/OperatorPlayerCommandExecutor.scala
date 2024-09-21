package org.noxet.noxetserver.commands.admin

import org.bukkit.command.{Command, CommandExecutor, CommandSender}
import org.bukkit.entity.Player
import org.noxet.noxetserver.commands.PlayerCommandExecutor
import org.noxet.noxetserver.messaging.ErrorMessage

trait OperatorPlayerCommandExecutor extends CommandExecutor:
  def onOperatorPlayerCommand(player: Player, command: Command, s: String, strings: Array[String]): Boolean

  final override def onCommand(commandSender: CommandSender, command: Command, s: String, strings: Array[String]): Boolean =
    commandSender match
      case p: Player if p.isOp => onOperatorPlayerCommand(p, command, s, strings)
      case _ =>
        ErrorMessage(ErrorMessage.ErrorType.Permission, "You must be an operator player to issue this command.").send(commandSender)
        true