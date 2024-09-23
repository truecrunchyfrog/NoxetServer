package org.noxet.noxetserver.playerstate

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player

trait PlayerStateProperty[T]:
  def getConfigName: String

  def getDefaultSerializedProperty: T

  def getSerializedPropertyFromPlayer(player: Player): T

  def restoreProperty(player: Player, value: T): Unit

  /**
   * Returns the value to be used when restoring from a configuration.
   * Does not need to be overridden for strings, booleans, integers, (array) lists, locations, vectors etc.
   * But is needed for floats, (sometimes) doubles, and certain cases such as for inventories and other more complex data structures.
   * Use the ConfigurationSection's methods (getDouble, getList ...) when possible.
   * Floats are by default given as doubles. Use {@code ((Double) config.getDouble(...)).floatValue()} in such cases.
   *
   * @param config the configuration to grab the value from.
   * @return the typed value that [[restoreProperty]] reads when restoring a player state from configuration.
   */
  def getValueFromConfig(config: ConfigurationSection): Option[T] =
    Option(config.get(getConfigName).asInstanceOf[T])