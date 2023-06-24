package org.noxet.noxetserver.minigames;

import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.generator.ChunkGenerator;
import org.noxet.noxetserver.minigames.worldeater.WorldEater;

import java.lang.reflect.InvocationTargetException;
import java.util.Random;

public enum GameDefinition {
    WORLD_EATER(WorldEater.class, new MiniGameOptions() {
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
        public WorldCreator getWorldCreator() {
            return voidWorldCreator;
        }

        @Override
        public GameMode getDefaultGameMode() {
            return GameMode.SURVIVAL;
        }

        @Override
        public SpectatorContract getSpectatorContract() {
            return null;
        }
    });

    public static final WorldCreator voidWorldCreator = new WorldCreator("void").generator(new ChunkGenerator() {
        @Override
        @SuppressWarnings({"NullableProblems", "deprecation"})
        public ChunkData generateChunkData(World world, Random random, int x, int z, BiomeGrid biome) {
            return createChunkData(world);
        }
    });

    private final MiniGameOptions options;
    private final Class<? extends MiniGameController> clazz;

    GameDefinition(Class<? extends MiniGameController> clazz, MiniGameOptions options) {
        this.clazz = clazz;
        this.options = options;
    }

    public MiniGameOptions getOptions() {
        return options;
    }

    public MiniGameController createGame() {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
