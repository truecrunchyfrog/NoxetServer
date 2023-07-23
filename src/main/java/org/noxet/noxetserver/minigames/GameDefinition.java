package org.noxet.noxetserver.minigames;

import org.bukkit.GameMode;
import org.noxet.noxetserver.minigames.worldeater.WorldEater;

public enum GameDefinition {
    WORLD_EATER(new MiniGameOptions() {
        @Override
        public String getId() {
            return "world-eater";
        }

        @Override
        public String getDisplayName() {
            return "World Eater";
        }

        @Override
        public int getMinPlayers() {
            return 2;
        }

        @Override
        public int getMaxPlayers() {
            return 8;
        }

        @Override
        public boolean allowPlayerDropIns() {
            return false;
        }

        @Override
        public GameMode getDefaultGameMode() {
            return GameMode.SURVIVAL;
        }

        @Override
        public SpectatorContract getSpectatorContract() {
            return SpectatorContract.ALL;
        }

        @Override
        public int getWorldChunksSquared() {
            return 5;
        }

        @Override
        public MiniGameController initGame() {
            return new WorldEater();
        }

        @Override
        public boolean shouldAnnounceAdvancements() {
            return true;
        }
    });

    private final MiniGameOptions options;

    GameDefinition(MiniGameOptions options) {
        this.options = options;
    }

    public MiniGameOptions getOptions() {
        return options;
    }

    public MiniGameController createGame() {
        return options.initGame();
    }
}
