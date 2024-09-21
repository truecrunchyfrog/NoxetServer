package org.noxet.noxetserver.commands.debug

import org.bukkit.command.Command
import org.bukkit.entity.Player
import org.noxet.noxetserver.combatlogging.CombatLogging
import org.noxet.noxetserver.commands.RegisteredCommand
import org.noxet.noxetserver.messaging.ErrorMessage

object FakeCombatLog extends OperatorPlayerCommandExecutor, RegisteredCommand("fake-combat-log", this):
  override def onOperatorPlayerCommand(player: Player, command: Command, s: String, strings: Array[String]): Boolean =
    CombatLogging.triggerCombatLog(player)
    true