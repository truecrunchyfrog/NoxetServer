package org.noxet.noxetserver.commands.admin

import org.bukkit.command.{Command, CommandExecutor, CommandSender}
import org.noxet.noxetserver.messaging.ErrorMessage

trait OperatorCommandExecutor extends CommandExecutor:
  def onOperatorCommand(commandSender: CommandSender, command: Command, s: String, strings: Array[String]): Boolean

  final override def onCommand(commandSender: CommandSender, command: Command, s: String, strings: Array[String]): Boolean =
    if commandSender.isOp then
      onOperatorCommand(commandSender, command, s, strings)
    else
      ErrorMessage(ErrorMessage.ErrorType.Permission, "You must be an operator to issue this command.").send(commandSender)
      true