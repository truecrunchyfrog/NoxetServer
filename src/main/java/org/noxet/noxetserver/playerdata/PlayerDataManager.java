package org.noxet.noxetserver.playerdata;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.noxet.noxetserver.NoxetServer;
import org.noxet.noxetserver.playerdata.types.PDTBoolean;
import org.noxet.noxetserver.playerdata.types.PDTMapStringMapStringLocation;
import org.noxet.noxetserver.playerdata.types.PDTStringList;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDataManager {
    private static final Map<UUID, String> configCache = new HashMap<>();

    public enum Attribute {
        HAS_DONE_CAPTCHA(new PDTBoolean()),
        TPA_BLOCKED_PLAYERS(new PDTStringList()),
        HOMES(new PDTMapStringMapStringLocation());

        private final PlayerDataType<?> type;

        Attribute(PlayerDataType<?> type) {
            this.type = type;
        }

        public String getKey() {
            return this.name().toLowerCase();
        }

        public PlayerDataType<?> getType() {
            return type;
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

    private static void updateCache(UUID uuid, YamlConfiguration config) {
        if(configCache.size() > 50)
            configCache.clear();
        configCache.put(uuid, config.saveToString());
    }

    private static YamlConfiguration getConfig(Player player) {
        if(configCache.containsKey(player.getUniqueId()))
            return YamlConfiguration.loadConfiguration(new StringReader(configCache.get(player.getUniqueId())));

        File dataFile = getDataFile(player);

        if(!dataFile.exists())
            return new YamlConfiguration();

        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);

        updateCache(player.getUniqueId(), config);

        return config;
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
        return attribute.getType().getValue(config, attribute.getKey());
    }

    public PlayerDataManager set(Attribute attribute, Object value) {
        config.set(attribute.getKey(), value);
        updateCache(player.getUniqueId(), config);

        return this;
    }

    public void save() {
        saveData(player, config);
    }
}
