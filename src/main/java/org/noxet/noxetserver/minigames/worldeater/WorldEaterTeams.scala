package org.noxet.noxetserver.minigames.worldeater

import org.bukkit.{ChatColor, Material}
import org.noxet.noxetserver.util.Team

enum WorldEaterTeams(
                      teamId: String,
                      displayName: String,
                      formattedDisplayName: String,
                      color: ChatColor,
                      teamIcon: Material) extends Team:
  case SEEKER extends WorldEaterTeams(
    "seekers",
    "Seeker",
    "§4§lSEEKER",
    ChatColor.RED,
    Material.STONE_AXE
  )

  case HIDER extends WorldEaterTeams(
    "hiders",
    "Hider",
    "§2§lHIDER",
    ChatColor.GREEN,
    Material.APPLE
  )

  override def getTeamId: String = teamId

  override def getDisplayName: String = displayName

  override def getFormattedDisplayName: String = formattedDisplayName + " "

  override def getColor: ChatColor = color

  override def getMaxTeamMembers: Int = 10

  override def getTeamIcon: Material = teamIcon