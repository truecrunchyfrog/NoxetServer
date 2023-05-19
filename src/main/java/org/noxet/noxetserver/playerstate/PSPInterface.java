package org.noxet.noxetserver.playerstate;

import org.bukkit.entity.Player;

public interface PSPInterface {
    String getConfigName();
    Object getSerializedPropertyFromPlayer(Player player);
    void restoreProperty(Player player, Object value);
}
