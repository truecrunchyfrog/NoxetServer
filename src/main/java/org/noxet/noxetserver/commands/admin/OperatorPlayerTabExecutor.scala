package org.noxet.noxetserver.commands.admin

import org.bukkit.command.{Command, CommandSender, TabExecutor}
import org.bukkit.entity.Player

import java.util
import scala.jdk.CollectionConverters.*

trait OperatorPlayerTabExecutor extends OperatorPlayerCommandExecutor, TabExecutor:
  def onOperatorPlayerTabComplete(player: Player, command: Command, s: String, strings: Array[String]): List[String]

  final override def onTabComplete(commandSender: CommandSender, command: Command, s: String, strings: Array[String]): util.List[String] =
    commandSender match
      case p: Player if p.isOp => onOperatorPlayerTabComplete(p, command, s, strings).asJava
      case _ => null