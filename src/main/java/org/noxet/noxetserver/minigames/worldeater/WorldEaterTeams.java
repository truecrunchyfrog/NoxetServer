package org.noxet.noxetserver.minigames.worldeater;

import org.bukkit.ChatColor;
import org.noxet.noxetserver.util.Team;

public enum WorldEaterTeams implements Team {
    SEEKER(
            "seekers",
            "Seeker",
            "§4§l[ SEEKER ]",
            ChatColor.RED
    ),

    HIDER(
            "hiders",
            "Hider",
            "§2§l[ HIDER ]",
            ChatColor.GREEN
    );

    private final String teamId, displayName, formattedDisplayName;
    private final ChatColor color;

    WorldEaterTeams(String teamId, String displayName, String formattedDisplayName, ChatColor color) {
        this.teamId = teamId;
        this.displayName = displayName;
        this.formattedDisplayName = formattedDisplayName;
        this.color = color;
    }

    @Override
    public String getTeamId() {
        return teamId;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getFormattedDisplayName() {
        return formattedDisplayName;
    }

    @Override
    public ChatColor getColor() {
        return color;
    }
}
