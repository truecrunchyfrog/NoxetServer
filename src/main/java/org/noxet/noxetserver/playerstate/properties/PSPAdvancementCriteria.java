package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.GameRule;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;
import org.noxet.noxetserver.NoxetServer;
import org.noxet.noxetserver.playerstate.PlayerStateProperty;

import java.util.Iterator;
import java.util.List;

import static org.noxet.noxetserver.playerstate.PlayerState.getAdvancementCriteriaList;

public class PSPAdvancementCriteria extends PlayerStateProperty {
    @Override
    public String getConfigName() {
        return "advancement_criteria";
    }

    @Override
    public Object getSerializedPropertyFromPlayer(Player player) {
        return getAdvancementCriteriaList(player);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void restoreProperty(Player player, Object value) {
        List<String> restoreCriteriaList = (List<String>) value;

        Iterator<Advancement> advancementIterator = NoxetServer.getPlugin().getServer().advancementIterator();

        boolean oldAnnouncementValue = Boolean.TRUE.equals(player.getWorld().getGameRuleValue(GameRule.ANNOUNCE_ADVANCEMENTS));
        player.getWorld().setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);

        while(advancementIterator.hasNext()) {
            AdvancementProgress progress = player.getAdvancementProgress(advancementIterator.next());
            for(String criteria : progress.getRemainingCriteria())
                if(restoreCriteriaList.contains(criteria))
                    progress.awardCriteria(criteria);
        }

        if(oldAnnouncementValue)
            player.getWorld().setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, true);
    }
}
