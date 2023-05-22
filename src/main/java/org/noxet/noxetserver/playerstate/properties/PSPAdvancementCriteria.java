package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.GameRule;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
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

        boolean oldAnnouncementValue = Boolean.TRUE.equals(player.getWorld().getGameRuleValue(GameRule.ANNOUNCE_ADVANCEMENTS));
        player.getWorld().setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);

        while(advancementIterator.hasNext()) {
            AdvancementProgress progress = player.getAdvancementProgress(advancementIterator.next());
            for(String criteria : progress.getRemainingCriteria())
                for(String hasCriteria : restoreCriteriaList)
                    if(criteria.equals(hasCriteria))
                        progress.awardCriteria(criteria);
        }

        if(oldAnnouncementValue)
            player.getWorld().setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, true);
    }

    @Override
    public Class<String[]> getTypeClass() {
        return String[].class;
    }
}
