package org.noxet.noxetserver.commands.debug

import org.bukkit.World
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.noxet.noxetserver.realm.RealmManager.{Realm, getCurrentRealm}
import org.noxet.noxetserver.messaging.ErrorMessage
import org.noxet.noxetserver.messaging.Message

class WhereAmI extends CommandExecutor:
    override def onCommand(commandSender: CommandSender, command: Command, s: String, strings: Array[String]): Boolean =
        commandSender match
            case player: Player =>
                val realm = getCurrentRealm(player)
                Message(s"World ID: ${player.getWorld.getName} @ Realm: " + realm.map(_.getDisplayName).getOrElse("Not in a realm")).send(player)
            case _ => ErrorMessage(ErrorMessage.ErrorType.COMMON, "Only players can check where they are.").send(commandSender)
        true