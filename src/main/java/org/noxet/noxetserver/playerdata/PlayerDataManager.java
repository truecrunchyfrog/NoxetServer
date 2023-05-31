package org.noxet.noxetserver.playerdata;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.noxet.noxetserver.NoxetServer;
import org.noxet.noxetserver.playerdata.types.PDTBoolean;
import org.noxet.noxetserver.playerdata.types.PDTMapStringMapStringLocation;
import org.noxet.noxetserver.playerdata.types.PDTStringList;

import java.io.File;
import java.io.IOException;

public class PlayerDataManager {

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
        return attribute.getType().getValue(config, attribute.getKey());
    }

    public PlayerDataManager set(Attribute attribute, Object value) {
        config.set(attribute.getKey(), value);
        return this;
    }

    public void save() {
        saveData(player, config);
    }
}
