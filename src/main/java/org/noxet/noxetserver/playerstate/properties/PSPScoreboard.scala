package org.noxet.noxetserver.playerstate.properties

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.bukkit.scoreboard.Scoreboard
import org.noxet.noxetserver.NoxetServer
import org.noxet.noxetserver.minigames.MiniGameManager
import org.noxet.noxetserver.playerstate.PlayerStateProperty

object PSPScoreboard extends PlayerStateProperty[Scoreboard]:
  override def getConfigName: String = "scoreboard"

  override def getDefaultSerializedProperty: Scoreboard = null

  override def getSerializedPropertyFromPlayer(player: Player): Scoreboard = null

  override def restoreProperty(player: Player, value: Scoreboard): Unit =
    // We only reset the scoreboard!
    if !MiniGameManager.isPlayerBusyInGame(player) then // Don't interrupt game scoreboards.
      player.setScoreboard(NoxetServer.getPlugin.getServer.getScoreboardManager.getNewScoreboard)

  override def getValueFromConfig(config: ConfigurationSection): Scoreboard = null