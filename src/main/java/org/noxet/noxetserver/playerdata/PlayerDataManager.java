package org.noxet.noxetserver.playerdata;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.noxet.noxetserver.NoxetServer;
import org.noxet.noxetserver.playerdata.types.*;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerDataManager {
    private static final Map<UUID, String> configCache = new HashMap<>();

    public enum Attribute {
        HAS_DONE_CAPTCHA(new PDTBoolean()),
        BLOCKED_PLAYERS(new PDTStringList()),
        MSG_SPOKEN_TO(new PDTStringList()),
        MSG_DISABLED(new PDTBoolean()),
        DISALLOW_INCOMING_FRIEND_REQUESTS(new PDTBoolean()),
        DISALLOW_INCOMING_TPA_REQUESTS(new PDTBoolean()),
        HOMES(new PDTMapStringMapStringLocation()),
        HAS_UNDERSTOOD_ANARCHY(new PDTBoolean()),
        SEEN_CHAT_NOTICE(new PDTBoolean()),
        MUTED(new PDTBoolean()),
        TIMES_JOINED(new PDTInteger()),
        SECONDS_PLAYED(new PDTInteger()),
        LAST_PLAYED(new PDTLong()),
        FRIEND_LIST(new PDTStringList()),
        INCOMING_FRIEND_REQUESTS(new PDTStringList()),
        OUTGOING_FRIEND_REQUESTS(new PDTStringList()),
        FRIEND_TELEPORTATION(new PDTBoolean()),
        SHOW_FRIEND_HOMES(new PDTBoolean()),
        CREEPER_SWEEPER_WINS(new PDTInteger()),
        CREEPER_SWEEPER_LOSSES(new PDTInteger()),
        CREEPER_SWEEPER_TOTAL_WIN_PLAYTIME(new PDTLong()),
        DISALLOW_INCOMING_PARTY_INVITES(new PDTBoolean());

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

    private static File getDataFile(UUID uuid) {
        return new File(getDirectory(), uuid + ".yml");
    }

    private static void updateCache(UUID uuid, YamlConfiguration config) {
        if(configCache.size() > 50)
            configCache.clear();
        configCache.put(uuid, config.saveToString());
    }

    public static void clearCacheForUUID(UUID uuid) {
        configCache.remove(uuid);
    }

    public static void clearAllCache() {
        configCache.clear();
    }

    private static YamlConfiguration getConfig(UUID uuid) {
        if(configCache.containsKey(uuid))
            return YamlConfiguration.loadConfiguration(new StringReader(configCache.get(uuid)));

        File dataFile = getDataFile(uuid);

        if(!dataFile.exists())
            return new YamlConfiguration();

        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);

        updateCache(uuid, config);

        return config;
    }

    private static void saveData(UUID uuid, YamlConfiguration config) {
        try {
            config.save(getDataFile(uuid));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private final UUID uuid;
    private final YamlConfiguration config;

    public PlayerDataManager(UUID uuid) {
        this.uuid = uuid;
        config = getConfig(uuid);
    }

    public PlayerDataManager(Player player) {
        this(player.getUniqueId());
    }

    public Object get(Attribute attribute) {
        return attribute.getType().getValue(config, attribute.getKey());
    }

    public PlayerDataManager set(Attribute attribute, Object value) {
        config.set(attribute.getKey(), value);
        updateCache(uuid, config);

        return this;
    }

    public PlayerDataManager toggleBoolean(Attribute attribute) {
        boolean oldValue = (boolean) get(attribute);
        return set(attribute, !oldValue);
    }

    public PlayerDataManager addInt(Attribute attribute, int addWith) {
        int oldValue = (int) get(attribute);
        return set(attribute, oldValue + addWith);
    }

    public PlayerDataManager addLong(Attribute attribute, long addWith) {
        long oldValue = (long) get(attribute);
        return set(attribute, oldValue + addWith);
    }

    public PlayerDataManager incrementInt(Attribute attribute) {
        return addInt(attribute, 1);
    }

    public PlayerDataManager remove(Attribute attribute) {
        return set(attribute, null);
    }

    public PlayerDataManager addToStringList(Attribute attribute, String value) {
        List<String> list = config.getStringList(attribute.getKey());
        list.add(value);
        return set(attribute, list);
    }

    public PlayerDataManager removeFromStringList(Attribute attribute, String value) {
        List<String> list = config.getStringList(attribute.getKey());
        list.remove(value);
        return set(attribute, list);
    }

    public boolean doesContain(Attribute attribute, Object value) {
        List<?> list = config.getList(attribute.getKey());
        return list != null && list.contains(value);
    }

    public int getListSize(Attribute attribute) {
        List<?> list = config.getList(attribute.getKey());
        return list != null ? list.size() : 0;
    }

    public void save() {
        saveData(uuid, config);
    }
}
