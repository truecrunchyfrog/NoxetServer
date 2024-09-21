package org.noxet.noxetserver.commands.teleportation

import org.bukkit.command.Command
import org.bukkit.entity.Player
import org.noxet.noxetserver.commands.{PlayerCommandExecutor, RegisteredCommand}
import org.noxet.noxetserver.realm.RealmManager.goToHub

object Hub extends PlayerCommandExecutor, RegisteredCommand("hub", this):
  override def onPlayerCommand(player: Player): Boolean =
    goToHub(player)
    true