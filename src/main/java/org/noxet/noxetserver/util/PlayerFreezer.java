package org.noxet.noxetserver.util;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PlayerFreezer extends DynamicTimer {
    private final Map<Player, Location> freezerMap;
    private final int tickFrequency;

    /**
     * Initializes a freezer instance with its own mapping.
     * @param tickFrequency The ticks to wait between teleports (lower value = faster teleports)
     */
    public PlayerFreezer(int tickFrequency) {
        freezerMap = new HashMap<>();
        this.tickFrequency = tickFrequency;
    }

    /**
     * Freeze this player to a specific location.
     * This will teleport the player at the given frequency to the given location.
     * @param player The player to freeze
     * @param location The location to freeze the player to
     */
    public void freeze(Player player, Location location) {
        freezerMap.put(player, location);
        touchTimer();
    }

    /**
     * Freeze this player to its current location.
     * This will teleport the player at the given frequency to its origin location upon freeze.
     * @param player The player to freeze
     */
    public void freeze(Player player) {
        freeze(player, player.getLocation());
    }

    /**
     * Freeze all players in a set.
     * @param players The players to freeze
     */
    public void bulkFreeze(Set<Player> players) {
        for(Player player : players)
            freeze(player);
    }

    /**
     * Stops freezing a player.
     * @param player The player to stop freezing
     */
    public void unfreeze(Player player) {
        freezerMap.remove(player);
        touchTimer();
    }

    /**
     * Check whether the player is in this freezer.
     * @param player The player to check
     * @return true if the player is in the freezer, otherwise false
     */
    public boolean isPlayerFrozen(Player player) {
        return freezerMap.containsKey(player);
    }

    @Override
    public boolean isTimerNecessary() {
        return !freezerMap.isEmpty();
    }

    @Override
    public int getTickFrequency() {
        return tickFrequency;
    }

    /**
     * Gets a set of the frozen players. Useful for when excluding frozen players from an event or similar.
     * @return A set of the currently frozen players in this freezer instance
     */
    public Set<Player> getFrozenPlayers() {
        return freezerMap.keySet();
    }

    /**
     * Empty (clear) this freezer. Removes all frozen players in this freezer.
     * By removing all players from the freezer, the timer will also be stopped.
     * Therefore, use this method for unregistering the timer event (by not touching it after empty).
     */
    public void empty() {
        freezerMap.clear();
        touchTimer();
    }

    @Override
    public void run() {
        for(Map.Entry<Player, Location> freezeEntry : freezerMap.entrySet())
            if(freezeEntry.getKey().getLocation() != freezeEntry.getValue() || freezeEntry.getKey().getLocation().getDirection() != freezeEntry.getValue().getDirection())
                freezeEntry.getKey().teleport(freezeEntry.getValue());
    }
}
