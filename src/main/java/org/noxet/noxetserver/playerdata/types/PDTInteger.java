package org.noxet.noxetserver.playerdata.types;

import org.bukkit.configuration.file.YamlConfiguration;
import org.noxet.noxetserver.playerdata.PlayerDataType;

public class PDTInteger implements PlayerDataType<Integer> {
    @Override
    public Integer getEmptyValue() {
        return 0;
    }

    @Override
    public Integer getValue(YamlConfiguration config, String key) {
        return config.getInt(key);
    }
}
