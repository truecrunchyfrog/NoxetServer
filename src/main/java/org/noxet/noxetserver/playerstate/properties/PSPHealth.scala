package org.noxet.noxetserver.playerstate.properties

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import org.noxet.noxetserver.NoxetServer
import org.noxet.noxetserver.playerstate.PlayerStateProperty

object PSPHealth extends PlayerStateProperty[Double]:
  override def getConfigName: String = "health"

  override def getDefaultSerializedProperty: Double = 20

  override def getSerializedPropertyFromPlayer(player: Player): Double =
    player.getHealth

  override def restoreProperty(player: Player, health: Double): Unit =
    // Restore health property on next tick,
    // to prevent stuck outside of realm bug (let teleport happen first).
    QuickRunnable(player.setHealth(health))
      .runTaskLater(NoxetServer.getPlugin, 1)

  override def getValueFromConfig(config: ConfigurationSection): Double =
    config.getDouble(getConfigName)