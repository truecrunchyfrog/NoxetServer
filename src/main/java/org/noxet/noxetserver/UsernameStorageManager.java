package org.noxet.noxetserver;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class UsernameStorageManager {
    /**
     * May be null. getUsernameConfig() should be used to safely read config.
     */
    private static YamlConfiguration usernameConfigCache;

    private static File getUsernamesFile() {
        File usernamesFile = new File(NoxetServer.getPlugin().getPluginDirectory(), "usernames.yml");

        try {
            if(!((usernamesFile.exists() && usernamesFile.isFile()) || usernamesFile.createNewFile()))
                throw new RuntimeException("Cannot find/create usernames file.");
        } catch(IOException e) {
            throw new RuntimeException(e);
        }

        return usernamesFile;
    }

    private static YamlConfiguration getFreshConfig() {
        return YamlConfiguration.loadConfiguration(getUsernamesFile());
    }

    private static YamlConfiguration getUsernameConfig() {
        if(usernameConfigCache == null)
            usernameConfigCache = getFreshConfig();

        return usernameConfigCache;
    }

    public static UUID getUUIDFromUsernameOrUUID(String usernameOrUUID) {
        YamlConfiguration config = getUsernameConfig();

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

    /**
     * Gets a player's real casing (when not knowing their real username, e.g. from player input).
     * @param username The case-insensitive username whose real username to get.
     * @return The player's real username if the player's username is registered, otherwise null.
     */
    public static String getCasedUsername(String username) {
        return getCasedUsernameFromUUID(getUUIDFromUsernameOrUUID(username));
    }

    public static String getCasedUsernameFromUUID(UUID uuid) {
        if(uuid == null)
            return null;

        OfflinePlayer offlinePlayer = NoxetServer.getPlugin().getServer().getOfflinePlayer(uuid);

        return offlinePlayer.getName(); // Username with casing if found, otherwise null.
    }

    public static void assignUUIDToUsername(String username, UUID uuid) {
        YamlConfiguration config = getUsernameConfig();

        config.set(username.toLowerCase(), uuid.toString());

        try {
            config.save(getUsernamesFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Updated the cache too, so a cache update is unnecessary.
    }
}
