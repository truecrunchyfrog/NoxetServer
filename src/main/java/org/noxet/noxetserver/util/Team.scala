package org.noxet.noxetserver.util

import org.bukkit.{ChatColor, Material}

trait Team:
  def getTeamId: String

  def getDisplayName: String

  def getFormattedDisplayName: String

  def getColor: ChatColor

  def getMaxTeamMembers: Int

  def getTeamIcon: Material