package org.noxet.noxetserver.util

import org.noxet.noxetserver.commands.social.Friend
import org.noxet.noxetserver.playerdata.PlayerDataManager
import org.noxet.noxetserver.playerstate.PlayerState

import java.util.UUID
import scala.jdk.CollectionConverters.*

object PlayerDataEraser extends ConfigManager:
    override protected def getFileName: String = "planned-data-deletions"

    def performDataErasureCheck(): Unit =
        for
            key <- config.getKeys(false)
            if config.getLong(key) > System.currentTimeMillis
        do
            config.set(key, null)
            save()
            eraseAllPlayerData(UUID.fromString(key))

    /**
     * Cancel a planned player data deletion if it exists, otherwise does nothing.
     * @param uuid The UUID of the player to cancel the planned data deletion for
     * @return true if plan existed and was canceled, otherwise false
     */
    def cancelPlayerDataErasePlan(uuid: UUID): Boolean =
        Option(config.get(uuid.toString)) match
            case Some(_) =>
                config.set(uuid.toString, null)
                save()
                true
            case None => false

    /**
     * Plans a data deletion for the player with the provided UUID.
     * @param uuid The UUID of the player to plan the data deletion for
     */
    def planDataErasure(uuid: UUID): Unit =
        config.set(uuid.toString, System.currentTimeMillis + 1000 * 60 * 60 * 24 * 3)

    /**
     * Delete all the saved player data.
     * Should be done when the player is offline, to prevent bugs and to sustain the erasure.
     *
     * @param uuid The UUID of the player who should have their data deleted.
     */
    def eraseAllPlayerData(uuid: UUID): Unit =
        // Clean up friends:
        
        Friend.getFriendList(uuid).foreach(Friend.removeFriend(uuid, UUID.fromString(_))) // Remove friends
        Friend.getIncomingFriendRequests(uuid).foreach(Friend.denyRequest(uuid, UUID.fromString(_))) // Remove incoming requests
        Friend.getOutgoingFriendRequests(uuid).foreach(Friend.denyRequest(UUID.fromString(_), uuid)) // Remove outgoing requests

        PlayerDataManager.deleteDataFile(uuid) // Delete entire data file.

        PlayerState.deleteStateFile(uuid) // Delete the player's saved states.