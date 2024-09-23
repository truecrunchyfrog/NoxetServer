package org.noxet.noxetserver.playerstate.properties

import org.bukkit.entity.Player
import org.noxet.noxetserver.playerstate.PlayerStateProperty

object PSPArrowsInBody extends PlayerStateProperty[Integer]:
  override def getConfigName: String = "arrows_in_body"

  override def getDefaultSerializedProperty: Integer = 0

  override def getSerializedPropertyFromPlayer(player: Player): Integer =
    player.getArrowsInBody

  override def restoreProperty(player: Player, arrows: Integer): Unit =
    player.setArrowsInBody(arrows)