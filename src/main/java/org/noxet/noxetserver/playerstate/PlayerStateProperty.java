package org.noxet.noxetserver.playerstate;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public interface PlayerStateProperty<T> {
    String getConfigName();
    T getDefaultSerializedProperty();
    T getSerializedPropertyFromPlayer(Player player);
    void restoreProperty(Player player, T value);

    /**
     * Returns the value to be used when restoring from a configuration.
     * Does not need to be overridden for strings, booleans, integers, (array) lists, locations, vectors etc.
     * But is needed for floats, (sometimes) doubles, and certain cases such as for inventories and other more complex situations.
     * Use the ConfigurationSection's methods (getDouble, getList ...) when possible.
     * Floats are by default given as doubles. Use {@code ((Double) config.getDouble(...)).floatValue()} in such cases.
     * @param config The configuration to grab the value from
     * @return The typed value that the restoreProperty reads when restoring a player state from configuration.
     */
    @SuppressWarnings("unchecked")
    default T getValueFromConfig(ConfigurationSection config) {
        return (T) config.get(getConfigName());
    }
}
