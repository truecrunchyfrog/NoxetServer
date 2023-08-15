package org.noxet.noxetserver.util;

import org.bukkit.ChatColor;
import org.bukkit.Material;

public interface Team {
    String getTeamId();
    String getDisplayName();
    String getFormattedDisplayName();
    ChatColor getColor();
    int getMaxTeamMembers();
    Material getTeamIcon();
}
