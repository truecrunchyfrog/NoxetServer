package org.noxet.noxetserver.minigames;

import org.bukkit.GameMode;

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

    GameMode getDefaultGameMode();

    SpectatorContract getSpectatorContract();

    int getWorldChunksSquared();

    MiniGameController initGame();
}
