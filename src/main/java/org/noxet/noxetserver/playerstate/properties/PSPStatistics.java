package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.noxet.noxetserver.playerstate.PlayerStateProperty;

import java.util.HashMap;
import java.util.Map;

public class PSPStatistics extends PlayerStateProperty {
    @Override
    public String getConfigName() {
        return "statistics";
    }

    @Override
    public Object getSerializedPropertyFromPlayer(Player player) {
        Map<String, Integer> untypedPlayerStatistics = new HashMap<>();
        Map<String, Map<String, Integer>> materialPlayerStatistics = new HashMap<>();
        Map<String, Map<String, Integer>> entityPlayerStatistics = new HashMap<>();

        for(Statistic statistic : Statistic.values()) {
            switch(statistic.getType()) {
                case UNTYPED: // Basic (untyped) statistics.
                    untypedPlayerStatistics.put(statistic.name(), player.getStatistic(statistic));
                    break;
                case BLOCK:
                case ITEM: // Material specific statistics.
                    materialPlayerStatistics.put(statistic.name(), new HashMap<>());

                    int materialStatistic;
                    for(Material material : Material.values())
                        if((materialStatistic = player.getStatistic(statistic, material)) != 0)
                            materialPlayerStatistics.get(statistic.name()).put(material.name(), materialStatistic);
                    break;
                case ENTITY: // Entity specific statistics.
                    entityPlayerStatistics.put(statistic.name(), new HashMap<>());

                    int entityStatistic;
                    for(EntityType entityType : EntityType.values()) {
                        try {
                            if((entityStatistic = player.getStatistic(statistic, entityType)) != 0)
                                entityPlayerStatistics.get(statistic.name()).put(entityType.name(), entityStatistic);
                        } catch(IllegalArgumentException e) {
                            // We expected this. I don't know how else to check whether an entity is statistical.
                        }
                    }
                    break;
            }
        }

        Map<String, Map<String, ?>> allPlayerStatistics = new HashMap<>();

        allPlayerStatistics.put("untyped", untypedPlayerStatistics);
        allPlayerStatistics.put("material", materialPlayerStatistics);
        allPlayerStatistics.put("entity", entityPlayerStatistics);

        return allPlayerStatistics;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void restoreProperty(Player player, Object value) {
        Map<String, Map<String, ?>> allPlayerStatistics = (Map<String, Map<String, ?>>) value;

        Map<String, Integer> untypedPlayerStatistics = (Map<String, Integer>) allPlayerStatistics.get("untyped");
        Map<String, Map<String, Integer>> materialPlayerStatistics = (Map<String, Map<String, Integer>>) allPlayerStatistics.get("material");
        Map<String, Map<String, Integer>> entityPlayerStatistics = (Map<String, Map<String, Integer>>) allPlayerStatistics.get("entity");

        for(Statistic statistic : Statistic.values()) {
            switch(statistic.getType()) {
                case UNTYPED:
                    player.setStatistic(statistic, untypedPlayerStatistics.get(statistic.name()));
                    break;
                case BLOCK:
                case ITEM:
                    if(materialPlayerStatistics.containsKey(statistic.name()))
                        for(Material material : Material.values())
                            player.setStatistic(statistic, material, materialPlayerStatistics.get(statistic.name()).getOrDefault(material.name(), 0));
                    break;
                case ENTITY:
                    if(entityPlayerStatistics.containsKey(statistic.name()))
                        for(EntityType entityType : EntityType.values()) {
                            try {
                                player.setStatistic(statistic, entityType, entityPlayerStatistics.get(statistic.name()).getOrDefault(entityType.name(), 0));
                            } catch(IllegalArgumentException e) {
                                // We expected this. I don't know how else to check whether an entity is statistical.
                            }
                        }
                    break;
            }
        }
    }
}
