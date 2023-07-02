package org.noxet.noxetserver.util;

import org.bukkit.ChatColor;

public interface Team {
    String getTeamId();
    String getDisplayName();
    String getFormattedDisplayName();
    ChatColor getColor();
}
