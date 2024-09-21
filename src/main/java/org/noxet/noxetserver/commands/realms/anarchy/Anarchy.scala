package org.noxet.noxetserver.commands.realms.anarchy

import org.bukkit.command.Command
import org.bukkit.entity.Player
import org.noxet.noxetserver.commands.{PlayerCommandExecutor, RegisteredCommand}
import org.noxet.noxetserver.realm.RealmManager.Realm.Anarchy
import org.noxet.noxetserver.realm.RealmManager.migrateToRealm

object Anarchy extends PlayerCommandExecutor, RegisteredCommand("anarchy", this):
    override def onPlayerCommand(player: Player, command: Command, s: String, strings: Array[String]): Boolean =
        migrateToRealm(player, Anarchy)
        true