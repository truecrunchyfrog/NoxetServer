package org.noxet.noxetserver.commands.teleportation

import org.bukkit.command.{Command, CommandExecutor, CommandSender}
import org.bukkit.entity.Player
import org.noxet.noxetserver.commands.{PlayerCommandExecutor, RegisteredCommand}
import org.noxet.noxetserver.messaging.ErrorMessage
import org.noxet.noxetserver.realm.RealmManager
import org.noxet.noxetserver.realm.RealmManager.{getCurrentRealm, goToSpawn}

object Spawn extends PlayerCommandExecutor, RegisteredCommand("spawn", this):
  override def onPlayerCommand(player: Player): Boolean =
    getCurrentRealm(player) match
      case Some(r) if !r.allowTeleportationMethods =>
        ErrorMessage(ErrorMessage.ErrorType.Common, "You cannot use /spawn in this realm.").send(player)
      case _ => goToSpawn(player)
    true