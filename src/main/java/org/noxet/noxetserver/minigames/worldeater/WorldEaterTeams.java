package org.noxet.noxetserver.minigames.worldeater;

import org.noxet.noxetserver.util.Team;

public enum WorldEaterTeams implements Team {
    SEEKER(
            "Seeker",
            "§4§l[ SEEKER ]"
    ),

    HIDER(
            "Hider",
            "§2§l[ HIDER ]"
    );

    private final String displayName, formattedDisplayName;

    WorldEaterTeams(String displayName, String formattedDisplayName) {
        this.displayName = displayName;
        this.formattedDisplayName = formattedDisplayName;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getFormattedDisplayName() {
        return formattedDisplayName;
    }
}
