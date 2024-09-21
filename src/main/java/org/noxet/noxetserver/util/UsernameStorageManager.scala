package org.noxet.noxetserver.util

import org.noxet.noxetserver.NoxetServer

import java.util.UUID
import scala.util.Try

object UsernameStorageManager extends ConfigManager:
  protected override def getFileName: String = "usernames"

  private def getUuidFromString(uuid: String): Option[UUID] = Try(UUID.fromString(uuid)).toOption

  def getUuidFromUsernameOrUuid(usernameOrUuid: String): Option[UUID] =
    getUuidFromString(
      Option(config.getString(usernameOrUuid.toLowerCase)).getOrElse(usernameOrUuid))

  def bindUsernameToUuid(username: String, uuid: UUID): Unit =
    config.set(username.toLowerCase, uuid.toString)
    save()

  def getCasedUsernameFromUuid(uuid: UUID): Option[String] =
    Option(NoxetServer.getPlugin.getServer.getOfflinePlayer(uuid).getName)