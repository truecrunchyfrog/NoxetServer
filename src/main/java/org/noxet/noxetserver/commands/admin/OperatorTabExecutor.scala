package org.noxet.noxetserver.commands.admin

import org.bukkit.command.{Command, CommandSender, TabExecutor}

import java.util
import scala.jdk.CollectionConverters.*

trait OperatorTabExecutor extends OperatorCommandExecutor, TabExecutor:
  def onOperatorTabComplete(commandSender: CommandSender, command: Command, s: String, strings: Array[String]): List[String]

  final override def onTabComplete(commandSender: CommandSender, command: Command, s: String, strings: Array[String]): util.List[String] =
    if commandSender.isOp then
      onOperatorTabComplete(commandSender, command, s, strings).asJava
    else
      null