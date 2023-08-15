package org.noxet.noxetserver.minigames.worldeater;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.noxet.noxetserver.util.Team;

public enum WorldEaterTeams implements Team {
    SEEKER(
            "seekers",
            "Seeker",
            "§4§lSEEKER",
            ChatColor.RED,
            Material.STONE_AXE
    ),

    HIDER(
            "hiders",
            "Hider",
            "§2§lHIDER",
            ChatColor.GREEN,
            Material.APPLE
    );

    private final String teamId, displayName, formattedDisplayName;
    private final ChatColor color;
    private final Material teamIcon;

    WorldEaterTeams(String teamId, String displayName, String formattedDisplayName, ChatColor color, Material teamIcon) {
        this.teamId = teamId;
        this.displayName = displayName;
        this.formattedDisplayName = formattedDisplayName;
        this.color = color;
        this.teamIcon = teamIcon;
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
        return formattedDisplayName + " ";
    }

    @Override
    public ChatColor getColor() {
        return color;
    }

    @Override
    public int getMaxTeamMembers() {
        return 10;
    }

    @Override
    public Material getTeamIcon() {
        return teamIcon;
    }
}
