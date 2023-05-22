package org.noxet.noxetserver.playerstate;

import org.bukkit.entity.Player;

public interface PlayerStateProperty<T> {
    String getConfigName();
    T getDefaultSerializedProperty();
    T getSerializedPropertyFromPlayer(Player player);
    void restoreProperty(Player player, T value);
    Class<T> getTypeClass();
}
