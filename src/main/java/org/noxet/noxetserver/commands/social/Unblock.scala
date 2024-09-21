package org.noxet.noxetserver.commands.social

import org.bukkit.command.{Command, CommandSender, TabExecutor}
import org.bukkit.entity.Player
import org.noxet.noxetserver.commands.{PlayerTabExecutor, RegisteredCommand}
import org.noxet.noxetserver.messaging.ErrorMessage.ErrorType.*
import org.noxet.noxetserver.messaging.{ErrorMessage, SuccessMessage}
import org.noxet.noxetserver.playerdata.PlayerDataManager
import org.noxet.noxetserver.util.{PlayerIntel, UsernameStorageManager}

import java.util.UUID

object Unblock extends PlayerTabExecutor, RegisteredCommand("unblock", this):
  override def onPlayerCommand(player: Player, command: Command, s: String, strings: Array[String]): Boolean =
    strings match
      case Array(playerIdentifier, _*) =>
        PlayerIntel(playerIdentifier) match
          case Some(playerToUnblock) =>
            val playerDataManager = PlayerDataManager(player)

            if !playerDataManager.doesContain(PlayerDataManager.Attribute.BlockedPlayers, playerToUnblock.uuid.toString) then
              ErrorMessage(Common, s"You have not blocked $playerToUnblock.").send(player)
              return true

            playerDataManager.removeFromStringList(PlayerDataManager.Attribute.BlockedPlayers, playerToUnblock.uuid.toString).save()

            SuccessMessage(s"$playerToUnblock is no longer blocked. They can now message, friend request and TPA you.").send(player)

            true
          case None =>
            ErrorMessage(Common, "That player is not registered.").send(player)
            true
      case _ =>
        ErrorMessage(Argument, "Missing argument: player to unblock.").send(player)
        true

  override def onPlayerTabComplete(player: Player, command: Command, s: String, strings: Array[String]): List[String] =
    strings match
      case Array(_) =>
        PlayerDataManager(player)
          .get(PlayerDataManager.Attribute.BlockedPlayers).asInstanceOf[List[String]]
          .flatMap(PlayerIntel)
          .map(_.username)
      case _ => List()