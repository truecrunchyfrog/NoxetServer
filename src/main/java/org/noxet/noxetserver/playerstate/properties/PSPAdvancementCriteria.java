package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.noxet.noxetserver.NoxetServer;
import org.noxet.noxetserver.playerstate.PlayerStateProperty;

import java.util.Iterator;

import static org.noxet.noxetserver.playerstate.PlayerState.getAdvancementCriteriaList;

public class PSPAdvancementCriteria implements PlayerStateProperty<String[]> {
    @Override
    public String getConfigName() {
        return "advancement_criteria";
    }

    @Override
    public String[] getDefaultSerializedProperty() {
        return new String[0];
    }

    @Override
    public String[] getSerializedPropertyFromPlayer(Player player) {
        return getAdvancementCriteriaList(player).toArray(new String[0]);
    }

    @Override
    public void restoreProperty(Player player, String[] restoreCriteriaList) {
        Iterator<Advancement> advancementIterator = NoxetServer.getPlugin().getServer().advancementIterator();

        while(advancementIterator.hasNext()) {
            AdvancementProgress progress = player.getAdvancementProgress(advancementIterator.next());

            for(String criteria : progress.getRemainingCriteria()) // Check all not yet rewarded criteria
                for(String hasCriteria : restoreCriteriaList) // Loop all criteria which should be set
                    if(criteria.equals(hasCriteria)) // Is a criteria which should be set, not set?
                        progress.awardCriteria(criteria);

            for(String existingCriteria : progress.getAwardedCriteria()) {
                boolean revokeCritera = true;

                for(String hasCriteria : restoreCriteriaList)
                    if(existingCriteria.equals(hasCriteria)) {
                        revokeCritera = false;
                        break;
                    }

                if(revokeCritera)
                    progress.revokeCriteria(existingCriteria);
            }
        }
    }

    @Override
    public String[] getValueFromConfig(ConfigurationSection config) {
        return config.getStringList(getConfigName()).toArray(new String[0]);
    }
}
