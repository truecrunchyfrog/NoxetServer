package org.noxet.noxetserver;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;

public class PlayerDataManager {
    public enum Attribute {
        HAS_DONE_CAPTCHA(false);

        private final Object defaultValue;

        Attribute(Object defaultValue) {
            this.defaultValue = defaultValue;
        }

        public String getKey() {
            return this.name().toLowerCase();
        }

        public Object getDefaultValue() {
            return defaultValue;
        }
    }

    private static File getDirectory() {
        File playerDataDir = new File(NoxetServer.getPlugin().getPluginDirectory(), "PlayerData");

        if(!playerDataDir.mkdir() && (!playerDataDir.exists() || !playerDataDir.isDirectory()))
            throw new RuntimeException("Cannot create PlayerData directory.");

        return playerDataDir;
    }

    private static File getDataFile(Player player) {
        return new File(getDirectory(), player.getUniqueId() + ".yml");
    }

    private static YamlConfiguration getConfig(Player player) {
        File dataFile = getDataFile(player);

        if(!dataFile.exists())
            return new YamlConfiguration();

        return YamlConfiguration.loadConfiguration(dataFile);
    }

    private static void saveData(Player player, YamlConfiguration config) {
        try {
            config.save(getDataFile(player));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private final Player player;
    private final YamlConfiguration config;

    public PlayerDataManager(Player player) {
        this.player = player;
        config = getConfig(player);
    }

    public Object get(Attribute attribute) {
        return config.get(attribute.getKey(), attribute.getDefaultValue());
    }

    public PlayerDataManager set(Attribute attribute, Object value) {
        config.set(attribute.getKey(), value);
        return this;
    }

    public void save() {
        saveData(player, config);
    }
}
