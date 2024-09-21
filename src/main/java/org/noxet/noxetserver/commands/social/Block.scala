package org.noxet.noxetserver.commands.social

import org.bukkit.command.{Command, CommandSender, TabExecutor}
import org.bukkit.entity.Player
import org.noxet.noxetserver.NoxetServer
import org.noxet.noxetserver.commands.{PlayerTabExecutor, RegisteredCommand}
import org.noxet.noxetserver.messaging.{ErrorMessage, NoteMessage, SuccessMessage}
import org.noxet.noxetserver.playerdata.PlayerDataManager
import org.noxet.noxetserver.util.{PlayerIntel, UsernameStorageManager}

import java.util.UUID

object Block extends PlayerTabExecutor, RegisteredCommand("block", this):
  override def onPlayerCommand(player: Player, command: Command, s: String, strings: Array[String]): Boolean =
    strings match
      case Array(playerIdentifier) =>
        PlayerIntel(playerIdentifier) match
          case Some(PlayerIntel(_, Some(`player`), _)) =>
            ErrorMessage(Common, "You cannot block yourself.").send(player)
            true
          case Some(playerToBlock) =>
            val playerDataManager = PlayerDataManager(player)

            if playerDataManager.doesContain(PlayerDataManager.Attribute.BlockedPlayers, uuidToBlock.toString) then
              ErrorMessage(Common, s"You have already blocked $playerToBlock.").send(player)
              return true

            if Friend.areFriends(player, playerToBlock) then
              ErrorMessage(ErrorMessage.ErrorType.Common, "You are friends with this player. Please unfriend them before you can block them.").send(player)
              return true

            if Friend.hasReceivedFriendRequestFrom(player, playerToBlock) then
              Friend.denyRequest(player, playerToBlock)
              NoteMessage(s"Friend request from $playerToBlock was automatically denied due to block.").send(player)

            if Friend.hasReceivedFriendRequestFrom(playerToBlock, player) then
              Friend.denyRequest(playerToBlock, player)
              NoteMessage(s"Friend request to $playerToBlock was canceled due to block.").send(player)

            if playerDataManager.getListSize(PlayerDataManager.Attribute.BlockedPlayers) >= 500 then
              ErrorMessage(ErrorMessage.ErrorType.Common, "You can only block up to 500 players.").send(player)
              return true

            playerDataManager.addToStringList(PlayerDataManager.Attribute.BlockedPlayers, playerToBlock.uuid.toString).save()

            SuccessMessage(s"$playerToBlock is now blocked. They can no longer message, friend request or TPA you.").send(player)

            true
          case None =>
            ErrorMessage(Common, "That player is not registered.").send(player)
            true
      case _ =>
        ErrorMessage(Argument, "Missing argument: player to block.").send(player)
        true

  override def onPlayerTabComplete(player: Player, command: Command, s: String, strings: Array[String]): List[String] =
    strings match
      case Array(playerName) =>
        Option(NoxetServer.getPlugin.getServer.getPlayer(playerName)).map(_.getName).toList
      case _ => List()