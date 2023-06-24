package org.noxet.noxetserver.minigames;

import org.bukkit.GameMode;
import org.bukkit.WorldCreator;

public interface MiniGameOptions {
    enum SpectatorContract {
        ALL,
        ONLY_DEAD_PLAYERS
    }

    String getId();
    String getDisplayName();

    int getMinPlayers();
    int getMaxPlayers();

    boolean allowPlayerDropIns();

    WorldCreator getWorldCreator();

    GameMode getDefaultGameMode();

    SpectatorContract getSpectatorContract();
}
