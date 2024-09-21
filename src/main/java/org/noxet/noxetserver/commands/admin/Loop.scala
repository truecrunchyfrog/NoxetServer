package org.noxet.noxetserver.commands.admin

import org.bukkit.command.{Command, CommandSender, TabExecutor}
import org.bukkit.entity.Player
import org.bukkit.help.{HelpMap, HelpTopic}
import org.noxet.noxetserver.NoxetServer
import org.noxet.noxetserver.commands.RegisteredCommand
import org.noxet.noxetserver.messaging.{ErrorMessage, Message}

import scala.jdk.CollectionConverters.*

object Loop extends OperatorPlayerTabExecutor, RegisteredCommand("loop", this):
  override def onOperatorPlayerCommand(player: Player, command: Command, s: String, strings: Array[String]): Boolean =
    strings match
      case Array(times) => times.toIntOption match
        case Some(amount) if amount >= 2 && amount <= 500 =>
          if strings.length < 2 || strings(1).isEmpty then
            ErrorMessage(Argument, "Missing command to loop.").send(player)
            return true

          if command.getName.equalsIgnoreCase(strings(1)) || command.getAliases.contains(strings(1).toLowerCase) then
            ErrorMessage(Argument, "Nested loops are not allowed.").send(player)
            return true

          val commandToRun = strings.drop(1).mkString(" ")

          Message(s"§3Looping command §b$amount§3 times: §b/$commandToRun").send(player)

          (0 until amount).foreach(_ => player.performCommand(commandToRun))

          Message("§3Loop finished.").send(player)

          true
        case None =>
          ErrorMessage(Argument, "Times to repeat must be an integer within 2-500 (inclusive).").send(player)
          true
      case _ =>
        ErrorMessage(Argument, "Missing argument: times to repeat.").send(player)
        true

  override def onOperatorPlayerTabComplete(player: Player, command: Command, s: String, strings: Array[String]): List[String] =
    strings match
      case Array(_) => List("5", "10", "20", "50", "100", "150", "300", "500")
      case Array(_, _) =>
        NoxetServer.getPlugin.getServer.getHelpMap.getHelpTopics.asScala
          .filter(c => c.getName.startsWith("/") && c.canSee(player))
          .map(_.getName.substring(1))
      case Array(_, cmdName, args*) =>
        Option(NoxetServer.getPlugin.getServer.getPluginCommand(cmdName)) match
          case Some(cmdToCheck) if cmdToCheck.testPermissionSilent(player) && cmdToCheck != command =>
            cmdToCheck.tabComplete(commandSender, cmdToCheck.getName, args, player.getLocation)
          case _ => List()