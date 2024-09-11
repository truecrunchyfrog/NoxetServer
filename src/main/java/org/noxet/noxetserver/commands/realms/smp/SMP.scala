package org.noxet.noxetserver.commands.realms.smp

import org.bukkit.command.{Command, CommandExecutor, CommandSender}
import org.bukkit.entity.Player
import org.noxet.noxetserver.messaging.ErrorMessage
import org.noxet.noxetserver.realm.RealmManager
import org.noxet.noxetserver.realm.RealmManager.migrateToRealm

class SMP extends CommandExecutor:
  override def onCommand(commandSender: CommandSender, command: Command, s: String, strings: Array[String]): Boolean =
    commandSender match
      case p: Player => migrateToRealm(p, RealmManager.Realm.SMP)
      case _ => ErrorMessage(ErrorMessage.ErrorType.COMMON, "Only players can be sent to the SMP server.").send(commandSender)
    true