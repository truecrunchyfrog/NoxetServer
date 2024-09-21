package org.noxet.noxetserver.util

import org.bukkit.entity.Player
import org.noxet.noxetserver.NoxetServer

import java.util.UUID
import scala.util.Try

case class PlayerIntel(uuid: UUID, playerOption: Option[Player], username: String):
  override def toString: String = username

object PlayerIntel:
  def apply(uuid: UUID): PlayerIntel =
    PlayerIntel(
      uuid,
      NoxetServer.getPlugin.getServer.getPlayer(uuid), // Only works if the player is online.
      UsernameStorageManager.getCasedUsernameFromUuid(uuid).get // This works anyway.
    )

  def apply(uuidOrUsername: String): Option[PlayerIntel] =
    UsernameStorageManager.getUuidFromUsernameOrUuid(uuidOrUsername).map(PlayerIntel)