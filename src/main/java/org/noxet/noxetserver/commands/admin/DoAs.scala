package org.noxet.noxetserver.commands.admin

import org.bukkit.command.{Command, CommandSender, TabExecutor}
import org.bukkit.entity.Player
import org.bukkit.help.{HelpMap, HelpTopic}
import org.noxet.noxetserver.NoxetServer
import org.noxet.noxetserver.commands.RegisteredCommand
import org.noxet.noxetserver.messaging.{ErrorMessage, Message, SuccessMessage}

import scala.jdk.CollectionConverters.*

object DoAs extends OperatorTabExecutor, RegisteredCommand("doas", this):
  override def onOperatorCommand(commandSender: CommandSender, command: Command, s: String, strings: Array[String]): Boolean =
    strings match
      case Array(playerName) =>
        Option(NoxetServer.getPlugin.getServer.getPlayer(playerName)) match
          case Some(player) =>
            val commandArgs = strings.drop(1) // Remove the player name argument.

            val commandToRun = NoxetServer.getPlugin.getServer.getPluginCommand(commandArgs(0))

            if command == commandToRun then
              ErrorMessage(ErrorMessage.ErrorType.Common, "Recursive DOAS not allowed!").send(commandSender)
              return true

            val commandWithArgs = commandArgs.mkString(" ")

            Message(s"Performing command as \"${doAsPlayer.getName}\": §o/$commandWithArgs").send(commandSender)
            doAsPlayer.performCommand(commandWithArgs)
            SuccessMessage(s"Command was performed on ${doAsPlayer.getName}.").send(commandSender)
            true
          case None =>
            ErrorMessage(ErrorMessage.ErrorType.Argument, "That is not a valid player.").send(commandSender)
            true
      case _ =>
        ErrorMessage(ErrorMessage.ErrorType.Argument, "Provide a player to run command as.").send(commandSender)
        true

  override def onOperatorTabComplete(commandSender: CommandSender, command: Command, s: String, strings: Array[String]): List[String] =
    strings match
      case Array(playerName, _*) =>
        Option(NoxetServer.getPlugin.getServer.getPlayer(playerName)) match
          case Some(player) =>
            strings match
              case Array(_) => List(player.getName)
              case Array(_, _) =>
                NoxetServer.getPlugin.getServer.getHelpMap.commandMap.getHelpTopics.asScala
                  .filter(c => c.getName.startsWith("/") && c.canSee(player))
                  .map(_.getName.substring(1))
              case Array(_, testCmd, args*) =>
                Option(NoxetServer.getPlugin.getServer.getPluginCommand(testCmd)) match
                  case Some(doAsCmd) if doAsCmd.testPermissionSilent(player) && doAsCmd != command =>
                    doAsCmd.tabComplete(
                      player,
                      doAsCommand.getName,
                      args,
                      player.getLocation).asScala
                  case None => List()
          case None => List()
      case _ => List()