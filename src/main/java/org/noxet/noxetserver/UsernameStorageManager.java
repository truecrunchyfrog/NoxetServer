package org.noxet.noxetserver;

import org.bukkit.OfflinePlayer;

import java.util.UUID;

public class UsernameStorageManager extends ConfigManager {
    public UUID getUUIDFromUsernameOrUUID(String usernameOrUUID) {
        String uuid = config.getString(usernameOrUUID.toLowerCase());

        if(uuid == null) {
            // Try parsing it as a UUID directly instead.
            try {
                return UUID.fromString(usernameOrUUID);
            } catch(IllegalArgumentException e) {
                return null;
            }
        }

        return UUID.fromString(uuid);
    }

    public static String getCasedUsernameFromUUID(UUID uuid) {
        if(uuid == null)
            return null;

        OfflinePlayer offlinePlayer = NoxetServer.getPlugin().getServer().getOfflinePlayer(uuid);

        return offlinePlayer.getName(); // Username with casing if found, otherwise null.
    }

    public void assignUUIDToUsername(String username, UUID uuid) {
        config.set(username.toLowerCase(), uuid.toString());
        save();
    }

    @Override
    protected String getFileName() {
        return "usernames";
    }
}
