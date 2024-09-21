package org.noxet.noxetserver.commands.admin.realm

import org.bukkit.command.{Command, CommandExecutor, CommandSender}
import org.bukkit.entity.Player
import org.noxet.noxetserver.commands.admin.OperatorPlayerCommandExecutor
import org.noxet.noxetserver.commands.{PlayerCommandExecutor, RegisteredCommand}
import org.noxet.noxetserver.messaging.{ErrorMessage, SuccessMessage}
import org.noxet.noxetserver.realm.RealmDataManager
import org.noxet.noxetserver.realm.RealmManager.getCurrentRealm

object ResetRealmSpawn extends OperatorPlayerCommandExecutor, RegisteredCommand("reset-realm-spawn", this):
  override def onOperatorPlayerCommand(player: Player, command: Command, s: String, strings: Array[String]): Boolean =
    RealmDataManager().setSoftSpawnLocation(getCurrentRealm(player), None)
    SuccessMessage("Erased spawn for realm.").send(player)
    true