package org.noxet.noxetserver.commands.debug

import org.bukkit.command.{Command, CommandExecutor, CommandSender}
import org.noxet.noxetserver.commands.RegisteredCommand
import org.noxet.noxetserver.messaging.{ErrorMessage, SuccessMessage}
import org.noxet.noxetserver.playerdata.PlayerDataManager

object ClearPlayerDataCache extends OperatorCommandExecutor, RegisteredCommand("clear-player-data-cache", this):
  override def onOperatorCommand(commandSender: CommandSender, command: Command, s: String, strings: Array[String]): Boolean =
    PlayerDataManager.clearAllCache()
    SuccessMessage("Cache for all player data was cleared.").send(commandSender)
    true