package org.noxet.noxetserver.playerdata.types;

import org.bukkit.Location;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.noxet.noxetserver.playerdata.PlayerDataType;

import java.util.HashMap;
import java.util.Map;

public class PDTMapStringMapStringLocation implements PlayerDataType<Map<String, Map<String, Location>>> {
    @Override
    public Map<String, Map<String, Location>> getEmptyValue() {
        return new HashMap<>();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Map<String, Location>> getValue(YamlConfiguration config, String key) {
        Map<String, Map<String, Location>> result = new HashMap<>();

        MemorySection memorySection = (MemorySection) config.get(key);
        assert memorySection != null;

        for(Map.Entry<String, Object> topEntries : memorySection.getValues(false).entrySet())
            result.put(topEntries.getKey(), (Map<String, Location>) topEntries.getValue());

        return result;
    }
}
