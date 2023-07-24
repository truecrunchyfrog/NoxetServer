package org.noxet.noxetserver.combatlogging;

import org.bukkit.entity.Player;
import org.noxet.noxetserver.util.ConfigManager;
import org.noxet.noxetserver.playerstate.PlayerState;
import org.noxet.noxetserver.realm.RealmManager;

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

    public void combatLogRejoin(Player player, RealmManager.Realm realm) {
        if(isCombatLogged(player, realm)) { // Check if player left while combat logged last time.
            setPlayerCombatLogMode(player, realm, false); // Reset combat log state.
            player.setHealth(0); // Kill player!
        }
    }
}
