package org.noxet.noxetserver.playerstate.properties

import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.noxet.noxetserver.playerstate.PlayerStateProperty

object PSPGameMode extends PlayerStateProperty[String]:
  override def getConfigName: String = "game_mode"

  override def getDefaultSerializedProperty: String = GameMode.SURVIVAL.name

  override def getSerializedPropertyFromPlayer(player: Player): String =
    player.getGameMode.name

  override def restoreProperty(player: Player, mode: String): Unit =
    player.setGameMode(GameMode.valueOf(mode))