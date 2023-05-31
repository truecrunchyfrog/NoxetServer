package org.noxet.noxetserver.playerdata.types;

import org.bukkit.configuration.file.YamlConfiguration;
import org.noxet.noxetserver.playerdata.PlayerDataType;

import java.util.Collections;
import java.util.List;

public class PDTStringList implements PlayerDataType<List<String>> {
    @Override
    public List<String> getEmptyValue() {
        return Collections.emptyList();
    }

    @Override
    public List<String> getValue(YamlConfiguration config, String key) {
        return config.getStringList(key);
    }
}
