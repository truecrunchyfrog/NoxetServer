package org.noxet.noxetserver.util

import org.bukkit.ChatColor
import org.bukkit.Material

trait Team:
    def getTeamId: String
    def getDisplayName: String
    def getFormattedDisplayName: String
    def getColor: ChatColor
    def getMaxTeamMembers: Int
    def getTeamIcon: Material