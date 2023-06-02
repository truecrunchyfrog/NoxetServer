package org.noxet.noxetserver.playerdata.types;

import org.bukkit.configuration.file.YamlConfiguration;
import org.noxet.noxetserver.playerdata.PlayerDataType;

public class PDTString implements PlayerDataType<String> {
    @Override
    public String getEmptyValue() {
        return null;
    }

    @Override
    public String getValue(YamlConfiguration config, String key) {
        return config.getString(key);
    }
}
