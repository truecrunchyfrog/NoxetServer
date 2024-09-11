package org.noxet.noxetserver.util

import org.bukkit.Location
import org.bukkit.entity.Player

import scala.collection.{Set, mutable}

/**
 * Initializes a freezer instance with its own mapping.
 *
 * @param tickFrequency The ticks to wait between teleports (lower value = faster teleports)
 */
class PlayerFreezer(val tickFrequency: Int) extends DynamicTimer:
    private val freezerMap = mutable.HashMap[Player, Location]()

    /**
     * Freeze this player to a specific location.
     * This will teleport the player at the given frequency to the given location.
     *
     * @param player The player to freeze
     * @param location The location to freeze the player to
     */
    def freeze(player: Player, location: Location): Unit =
        freezerMap.put(player, location)
        touchTimer()

    /**
     * Freeze this player to its current location.
     * This will teleport the player at the given frequency to its origin location upon freeze.
     *
     * @param player The player to freeze
     */
    def freeze(player: Player): Unit = freeze(player, player.getLocation)

    /**
     * Freeze all players in a set.
     *
     * @param players The players to freeze
     */
    def bulkFreeze(players: Set[Player]): Unit = players.foreach(freeze)

    /**
     * Stops freezing a player.
     *
     * @param player The player to stop freezing
     */
    def unfreeze(player: Player): Unit =
        freezerMap.remove(player)
        touchTimer()

    /**
     * Check whether the player is in this freezer.
     *
     * @param player The player to check
     * @return true if the player is in the freezer, otherwise false
     */
    def isPlayerFrozen(player: Player): Boolean = freezerMap.containsKey(player)

    override def isTimerNecessary: Boolean = freezerMap.nonEmpty

    override def timerCall(): Unit =
        freezerMap.entrySet
          .filterNot(_.getKey.getLocation.toVector.subtract(_.getValue.toVector).isZero)
          .foreach(_.getKey.teleport(_.getValue))

    /**
     * Gets a set of the frozen players. Useful for when excluding frozen players from an event or similar.
     *
     * @return A set of the currently frozen players in this freezer instance
     */
    def getFrozenPlayers: Set[Player] = freezerMap.keySet

    /**
     * Empty (clear) this freezer. Removes all frozen players in this freezer.
     * By removing all players from the freezer, the timer will also be stopped.
     * Therefore, use this method for unregistering the timer event (by not touching it after empty).
     */
    def empty(): Unit =
        freezerMap.clear()
        touchTimer()