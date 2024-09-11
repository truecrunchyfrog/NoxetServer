package org.noxet.noxetserver.commands.admin.realm;

import org.bukkit.command.{Command, CommandExecutor, CommandSender}
import org.bukkit.entity.Player
import org.noxet.noxetserver.messaging.{ErrorMessage, SuccessMessage}
import org.noxet.noxetserver.realm.RealmDataManager
import org.noxet.noxetserver.realm.RealmManager.getCurrentRealm;

class SetRealmSpawn extends CommandExecutor:
    override def onCommand(commandSender: CommandSender, command: Command, s: String, strings: Array[String]): Boolean =
        if !commandSender.isInstanceOf[Player] then
            ErrorMessage(ErrorMessage.ErrorType.COMMON, "Only players can set realm spawns.").send(commandSender)
            return true

        val player = commandSender.asInstanceOf[Player]

        if !player.isOp then
            ErrorMessage(ErrorMessage.ErrorType.PERMISSION, "Only operators can set realm spawns.").send(commandSender)
            return true

        RealmDataManager().setSoftSpawnLocation(getCurrentRealm(player), player.getLocation())
        SuccessMessage("Updated spawn for realm.").send(player)

        true