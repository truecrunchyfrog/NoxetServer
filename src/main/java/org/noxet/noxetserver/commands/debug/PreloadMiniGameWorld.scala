package org.noxet.noxetserver.commands.debug

import org.bukkit.World
import org.bukkit.command.{Command, CommandExecutor, CommandSender}
import org.noxet.noxetserver.NoxetServer
import org.noxet.noxetserver.commands.RegisteredCommand
import org.noxet.noxetserver.messaging.ErrorMessage.ErrorType.Common
import org.noxet.noxetserver.messaging.{ErrorMessage, SuccessMessage}
import org.noxet.noxetserver.minigames.MiniGameController
import org.noxet.noxetserver.util.FancyTimeConverter

object PreloadMiniGameWorld extends OperatorCommandExecutor, RegisteredCommand("preload-mini-game-world", this):
  override def onOperatorCommand(commandSender: CommandSender, command: Command, s: String, strings: Array[String]): Boolean =
    val alreadyLoaded = List(NoxetServer.getPlugin.getServer.getWorlds)

    val initTimestamp = System.currentTimeMillis

    // Calling this method will load the world (and also create it if not existing).
    val miniGameWorld = MiniGameController.getMiniGameWorld

    if !alreadyLoaded.contains(miniGameWorld) then
      SuccessMessage(
        s"Successfully loaded mini-game world in " +
          FancyTimeConverter.deltaSecondsToFancyTime((System.currentTimeMillis - initTimestamp).toInt / 1000) +
          ".")
        .send(commandSender)
    else
      ErrorMessage(Common, "Mini-game world is already loaded.").send(commandSender)

    true