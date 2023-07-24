package org.noxet.noxetserver.util;

import org.noxet.noxetserver.commands.social.Friend;
import org.noxet.noxetserver.playerdata.PlayerDataManager;
import org.noxet.noxetserver.playerstate.PlayerState;

import java.util.UUID;

public class PlayerDataEraser extends ConfigManager {
    /**
     * Delete all the saved player data.
     * Should be done when the player is offline, to prevent bugs and to sustain the erasure.
     * @param uuid The UUID of the player who should have their data deleted.
     */
    public static void eraseAllPlayerData(UUID uuid) {
        // Clean up friends:

        for(String friendUUID : Friend.getFriendList(uuid)) // Remove friends
            Friend.removeFriend(uuid, UUID.fromString(friendUUID));

        for(String incomingUUID : Friend.getIncomingFriendRequests(uuid)) // Remove "to" requests
            Friend.denyRequest(uuid, UUID.fromString(incomingUUID));

        for(String outgoingUUID : Friend.getOutgoingFriendRequests(uuid)) // Remove "from" requests
            Friend.denyRequest(UUID.fromString(outgoingUUID), uuid);

        PlayerDataManager.deleteDataFile(uuid); // Delete entire data file.

        PlayerState.deleteStateFile(uuid); // Delete the player's saved states.
    }

    public void performDataErasureCheck() {
        for(String key : config.getKeys(false))
            if(config.getLong(key) > System.currentTimeMillis()) {
                config.set(key, null);
                save();
                eraseAllPlayerData(UUID.fromString(key));
            }
    }

    /**
     * Cancel a planned player data deletion if it exists, otherwise does nothing.
     * @param uuid The UUID of the player to cancel the planned data deletion for
     * @return true if plan existed and was canceled, otherwise false
     */
    public boolean cancelPlayerDataErasePlan(UUID uuid) {
        if(config.get(uuid.toString()) != null) {
            config.set(uuid.toString(), null);
            save();
            return true;
        }

        return false;
    }

    /**
     * Plans a data deletion for the player with the provided UUID.
     * @param uuid The UUID of the player to plan the data deletion for
     */
    public void planDataErasure(UUID uuid) {
        //                                                       ms     sec  min  hrs  days
        config.set(uuid.toString(), System.currentTimeMillis() + 1000 * 60 * 60 * 24 * 3);
    }

    @Override
    protected String getFileName() {
        return "planned-data-deletions";
    }
}
