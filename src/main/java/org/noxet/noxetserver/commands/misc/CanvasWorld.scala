package org.noxet.noxetserver.commands.misc

import org.bukkit.command.Command
import org.bukkit.entity.Player
import org.noxet.noxetserver.commands.RegisteredCommand
import org.noxet.noxetserver.commands.admin.OperatorPlayerCommandExecutor
import org.noxet.noxetserver.messaging.ErrorMessage
import org.noxet.noxetserver.realm.RealmManager.Realm.Canvas
import org.noxet.noxetserver.realm.RealmManager.migrateToRealm

object CanvasWorld extends OperatorPlayerCommandExecutor, RegisteredCommand("canvas-world", this):
  override def onOperatorPlayerCommand(player: Player): Boolean =
    migrateToRealm(commandSender, Canvas)
    true