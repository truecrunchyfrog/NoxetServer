package org.noxet.noxetserver.playerstate

import org.bukkit.GameMode
import org.bukkit.advancement.Advancement
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.noxet.noxetserver.NoxetServer
import org.noxet.noxetserver.menus.inventorysetups.HubInventorySetup

import java.io.{File, IOException}
import java.util.{ArrayList, Iterator, UUID}
import scala.jdk.CollectionConverters.*

object PlayerState:
  /**
   * A player state type. Every player can save one state per state type.
   */
  trait PlayerStateType:
    val id: String

  /**
   * Gets the PlayerStates directory, which contains all the saved states of players.
   *
   * @return The "PlayerStates" directory inside the plugin directory
   */
  private def getDirectory: File =
    val playerStateDir = File(NoxetServer.getPlugin.getPluginDirectory, "PlayerStates")

    if !playerStateDir.mkdir &&
      (!playerStateDir.exists || !playerStateDir.isDirectory) then
      throw RuntimeException("Cannot create PlayerStates directory.")

    playerStateDir


  private def getStateFile(uuid: UUID): File =
    File(getDirectory, s"$uuid.yml")

  def deleteStateFile(uuid: UUID): Unit = getStateFile(uuid).delete()

  private def getConfig(uuid: UUID): YamlConfiguration =
    YamlConfiguration.loadConfiguration(getStateFile(uuid))

  /**
   * Get a certain state section from a player state config. Each state section has its own properties.
   *
   * @param config    The config to get the state section from
   * @param stateType What section to return
   * @return The saved state if found, otherwise create it and return a blank section
   */
  private def getConfigSection(config: YamlConfiguration, stateType: PlayerStateType): ConfigurationSection =
    Option(config.getConfigurationSection(stateType.name))
      .getOrElse(config.createSection(stateType.name))

  /**
   * Restore a player's state.
   *
   * @param player    The player to restore
   * @param stateType The state to restore from
   */
  def restoreState(player: Player, stateType: PlayerStateType): Unit =
    if !player.isOnline then return

    prepareDefault(player)

    if !hasState(player, stateType) then // Player has no saved state!
      return // Return to skip attempt to restore from config (and to not restore null values).

    PSPManager.restoreFromConfiguration(
      getConfigSection(
        getConfig(player), stateType
      ), player
    )

  /**
   * Whether the player has a state saved with the provided type or not.
   *
   * @param player    The player to check whether the state exists on
   * @param stateType The state to see
   * @return true if a state with the specified type exists, otherwise false
   */
  def hasState(player: Player, stateType: PlayerStateType): Boolean =
    getConfig(player).isConfigurationSection(stateType.name)

  /**
   * Saves the player's current state in-game (inventory, health, etc.) to the given YAML configuration. This does NOT mean that the file is saved!
   * It must be saved to the file after saving to the config using this method.
   *
   * @param config    The config to save the player's current state to
   * @param player    The player whose state should be saved
   * @param stateType What state type/section of the config it should be saved to
   */
  private def saveStateToConfig(config: YamlConfiguration, player: Player, stateType: PlayerStateType): Unit =
    PSPManager.addToConfiguration(getConfigSection(config, stateType), player)

  /**
   * Saves the player's current state to their own state file (saved to plugins/NoxetServer/PlayerStates/uuid.yml where uuid is the player's UUID).
   *
   * @param player    The player whose state should be saved to disk
   * @param stateType What state section to save it to
   */
  def saveState(player: Player, stateType: PlayerStateType): Unit =
    val config = getConfig(player)
    saveStateToConfig(config, player, stateType)
    config.save(getStateFile(player))

  /**
   * Reset a player's state to "factory" settings.
   *
   * @param player The player to reset
   */
  def prepareDefault(player: Player): Unit =
    if player.isDead then
      player.spigot.respawn()

    PSPManager.restoreToDefault(player)

  /**
   * Set a player in the "idle" mode (invulnerable, invisible, flying).
   *
   * @param player  The player to prepare to idle mode
   * @param inherit Whether to reset the player completely
   */
  def prepareIdle(player: Player, inherit: Boolean): Unit =
    if inherit then
      prepareDefault(player)

    player.setInvulnerable(true)
    player.setInvisible(true)
    player.setCollidable(false)

    player.setAllowFlight(true)
    player.setFlying(true)

  /**
   * Set a player in the "normal" mode (vulnerable, visible, cannot fly).
   *
   * @param player  The player to set to normal mode
   * @param inherit Whether to reset the player completely
   */
  def prepareNormal(player: Player, inherit: Boolean): Unit =
    if inherit then
      prepareDefault(player)

    player.setInvulnerable(false)
    player.setInvisible(false)
    player.setCollidable(true)

    player.setAllowFlight(false)
    player.setFlying(false)

  def prepareHubState(player: Player): Unit =
    prepareDefault(player)

    player.setAllowFlight(true)
    player.setGameMode(GameMode.SURVIVAL)
    player.getInventory.setHeldItemSlot(3)
    HubInventorySetup.applyToPlayer(player)

  /**
   * Gets the advancement criteria for a player. Used for saving player states.
   *
   * @param player The player to get the criteria from
   * @return The advancement criteria as String[] (serializable to config)
   */
  def getAdvancementCriteriaList(player: Player): List[String] =
    NoxetServer.getPlugin.getServer.advancementIterator.asScala
      .flatMap(adv => player.getAdvancementProgress(adv).getAwardedCriteria)