package org.noxet.noxetserver.playerdata.types;

import org.bukkit.configuration.file.YamlConfiguration;
import org.noxet.noxetserver.playerdata.PlayerDataType;

public class PDTBoolean implements PlayerDataType<Boolean> {
    @Override
    public Boolean getEmptyValue() {
        return false;
    }

    @Override
    public Boolean getValue(YamlConfiguration config, String key) {
        return config.getBoolean(key);
    }
}
