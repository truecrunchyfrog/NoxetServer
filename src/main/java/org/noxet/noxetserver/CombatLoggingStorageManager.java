package org.noxet.noxetserver;

import org.bukkit.entity.Player;
import org.noxet.noxetserver.playerstate.PlayerState;

import java.util.List;

public class CombatLoggingStorageManager extends ConfigManager {
    @Override
    protected String getFileName() {
        return "combat-logging";
    }

    public void setPlayerCombatLogMode(Player player, RealmManager.Realm realm, boolean combatLogged) {
        String key = (realm != null ? realm.getPlayerStateType() : PlayerState.PlayerStateType.GLOBAL).name();

        List<String> realmCombatList = config.getStringList(key);

        if(combatLogged)
            realmCombatList.add(player.getUniqueId().toString());
        else
            realmCombatList.remove(player.getUniqueId().toString());
        config.set(key, realmCombatList);
        save();
    }

    public boolean isCombatLogged(Player player, RealmManager.Realm realm) {
        String key = (realm != null ? realm.getPlayerStateType() : PlayerState.PlayerStateType.GLOBAL).name();
        return config.getStringList(key).contains(player.getUniqueId().toString());
    }
}
