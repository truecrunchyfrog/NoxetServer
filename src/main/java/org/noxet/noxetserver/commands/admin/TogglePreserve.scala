package org.noxet.noxetserver.commands.admin

import org.bukkit.command.{Command, CommandExecutor, CommandSender}
import org.noxet.noxetserver.NoxetServer
import org.noxet.noxetserver.commands.RegisteredCommand
import org.noxet.noxetserver.messaging.{ErrorMessage, SuccessMessage}

object TogglePreserve extends OperatorCommandExecutor, RegisteredCommand("toggle-preserve", this):
  override def onOperatorCommand(commandSender: CommandSender, command: Command, s: String, strings: Array[String]): Boolean =
    NoxetServer.shouldAllowWorldPreservation ^= true
    SuccessMessage(
      s"${if NoxetServer.shouldAllowWorldPreservation then "Enabled" else "Disabled"} world preservation."
    ).send(commandSender)
    true