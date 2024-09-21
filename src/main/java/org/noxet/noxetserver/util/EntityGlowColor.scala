package org.noxet.noxetserver.util

import org.bukkit.entity.Entity
import org.bukkit.{Bukkit, ChatColor}

object EntityGlowColor:
  /**
   * Set the glow color of an entity (this will NOT make the entity glow, use Entity#setGlowing).
   *
   * @param entity The entity to set the glow color for
   */
  def setGlowColor(entity: Entity, color: ChatColor): Unit =
    val scoreboard = Bukkit.getScoreboardManager.getNewScoreboard
    val team = scoreboard.registerNewTeam("color")

    team.setColor(color)
    team.addEntry(entity.getName)