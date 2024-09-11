package org.noxet.noxetserver.util

import org.noxet.noxetserver.NoxetServer

import java.util.UUID

class UsernameStorageManager extends ConfigManager:
    protected override def getFileName: String = "usernames"

    private def getUuidFromString(uuid: String): Option[UUID] =
        try
            Some(UUID.fromString(uuid))
        catch
            case _: IllegalArgumentException => None

    def getUuidFromUsernameOrUuid(usernameOrUuid: String): Option[UUID] =
        val rawUuid = config.getString(usernameOrUuid.toLowerCase()) match
            // Username is not listed in database. Try parsing it as a UUID directly instead.
            case null => usernameOrUuid
            case uuid => uuid
        getUuidFromString(rawUuid)

    def bindUsernameToUuid(username: String, uuid: UUID): Unit =
        config.set(username.toLowerCase(), uuid.toString)
        save()

object UsernameStorageManager:
    def getCasedUsernameFromUuid(uuid: UUID): Option[String] =
        NoxetServer.getPlugin.getServer.getOfflinePlayer(uuid).getName match
            case null => None
            case name => Some(name)