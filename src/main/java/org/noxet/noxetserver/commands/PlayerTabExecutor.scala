package org.noxet.noxetserver.commands

import org.bukkit.command.{Command, CommandSender, TabExecutor}
import org.bukkit.entity.Player

import java.util
import scala.jdk.CollectionConverters.*

trait PlayerTabExecutor extends PlayerCommandExecutor, TabExecutor:
  def onPlayerTabComplete(player: Player, command: Command, s: String, strings: Array[String]): List[String]

  final override def onTabComplete(commandSender: CommandSender, command: Command, s: String, strings: Array[String]): util.List[String] =
    commandSender match
      case p: Player => onPlayerTabComplete(p, command, s, strings).asJava
      case _ => null