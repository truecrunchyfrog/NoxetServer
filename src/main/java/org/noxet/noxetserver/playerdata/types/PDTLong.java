package org.noxet.noxetserver.playerdata.types;

import org.bukkit.configuration.file.YamlConfiguration;
import org.noxet.noxetserver.playerdata.PlayerDataType;

public class PDTLong implements PlayerDataType<Long> {
    @Override
    public Long getEmptyValue() {
        return 0L;
    }

    @Override
    public Long getValue(YamlConfiguration config, String key) {
        return config.getLong(key);
    }
}
