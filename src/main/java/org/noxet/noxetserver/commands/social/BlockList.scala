package org.noxet.noxetserver.commands.social

import org.bukkit.command.{Command, CommandExecutor, CommandSender}
import org.bukkit.entity.Player
import org.noxet.noxetserver.commands.{PlayerCommandExecutor, RegisteredCommand}
import org.noxet.noxetserver.messaging.{ErrorMessage, Message}
import org.noxet.noxetserver.playerdata.PlayerDataManager
import org.noxet.noxetserver.util.UsernameStorageManager

import java.util.UUID

object BlockList extends PlayerCommandExecutor, RegisteredCommand("block-list", this):
  override def onPlayerCommand(player: Player): Boolean =
    val blockedUuids = PlayerDataManager(player).get(PlayerDataManager.Attribute.BlockedPlayers).asInstanceOf[List[String]]

    Message(s"§eBlocked players: ${blockedUuids.size}").send(player)

    blockedUuids
      .map(PlayerIntel)
      .foreach(blocked =>
        Message(s"└§4§lBLOCKED §c$blocked")
          .addButton("Pardon", ChatColor.GREEN, "Unblock this player", s"unblock $blocked")
          .send(player)
      )

    true