package org.noxet.noxetserver.playerdata;

import org.bukkit.configuration.file.YamlConfiguration;

public interface PlayerDataType<T> {
    T getEmptyValue();
    T getValue(YamlConfiguration config, String key);
}
