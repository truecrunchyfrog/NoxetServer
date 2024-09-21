package org.noxet.noxetserver.commands.realms.smp

import org.bukkit.command.Command
import org.bukkit.entity.Player
import org.noxet.noxetserver.commands.{PlayerCommandExecutor, RegisteredCommand}
import org.noxet.noxetserver.realm.RealmManager.Realm.Smp
import org.noxet.noxetserver.realm.RealmManager.migrateToRealm

object Smp extends PlayerCommandExecutor, RegisteredCommand("smp", this):
  override def onPlayerCommand(player: Player, command: Command, s: String, strings: Array[String]): Boolean =
    migrateToRealm(p, Smp)
    true