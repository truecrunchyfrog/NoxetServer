package org.noxet.noxetserver.commands.debug

import org.bukkit.World
import org.bukkit.command.Command
import org.bukkit.entity.Player
import org.noxet.noxetserver.commands.{PlayerCommandExecutor, RegisteredCommand}
import org.noxet.noxetserver.messaging.{ErrorMessage, Message}
import org.noxet.noxetserver.realm.RealmManager.{Realm, getCurrentRealm}

object WhereAmI extends PlayerCommandExecutor, RegisteredCommand("whereami", this):
  override def onPlayerCommand(player: Player): Boolean =
    Message(
      s"World ID: ${player.getWorld.getName} @ Realm: " +
        getCurrentRealm(player).map(_.getDisplayName).getOrElse("Not in a realm"))
      .send(player)
    true