package org.noxet.noxetserver;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public abstract class ConfigManager {
    protected YamlConfiguration config;
    private static final Map<String, YamlConfiguration> cache = new HashMap<>();

    protected abstract String getFileName();
    protected String getFileNameWithExtension() {
        return getFileName() + ".yml";
    }

    protected File getFile() {
        File configFile = new File(NoxetServer.getPlugin().getPluginDirectory(), getFileNameWithExtension());

        try {
            if(!((configFile.exists() && configFile.isFile()) || configFile.createNewFile()))
                throw new RuntimeException("Cannot find/create " + getFileNameWithExtension() + " file.");
        } catch(IOException e) {
            throw new RuntimeException(e);
        }

        return configFile;
    }

    protected YamlConfiguration getUncachedConfig() {
        return YamlConfiguration.loadConfiguration(getFile());
    }

    /**
     * Gives the cached configuration if it exists.
     * @return The cached YAML configuration if it exists, otherwise null
     */
    protected YamlConfiguration getCachedConfig() {
        return cache.get(getFileName());
    }

    protected void updateCache() {
        cache.put(getFileName(), getUncachedConfig());
    }

    /**
     * Gives a YAML configuration. Cached if available, otherwise uncached.
     * @return The YAML configuration, cached/uncached
     */
    protected YamlConfiguration getConfig() {
        if(getCachedConfig() == null)
            updateCache();
        return getCachedConfig();
    }

    public ConfigManager() {
        config = getConfig();
    }

    protected void save() {
        try {
            config.save(getFile());
            cache.remove(getFileName());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        config = getConfig();
    }
}
