package org.noxet.noxetserver;

import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import org.noxet.noxetserver.commands.CommandRegistration;
import org.noxet.noxetserver.messaging.Motd;
import org.noxet.noxetserver.realm.RealmManager;

import java.io.File;
import java.util.Random;

public final class NoxetServer extends JavaPlugin {

    private static NoxetServer plugin;

    public enum WorldFlag {
        NEUTRAL, OVERWORLD, NETHER, THE_END, FLAT, VOID
    }

    public static boolean shouldAllowWorldPreservation = true;

    public enum ServerWorld {
        HUB("hub", null, true, true, WorldFlag.NEUTRAL),
        SMP_SPAWN("smp_spawn", RealmManager.Realm.SMP, true, true, WorldFlag.NEUTRAL),
        SMP_WORLD("smp_world", RealmManager.Realm.SMP, false, false, WorldFlag.OVERWORLD),
        SMP_NETHER("smp_nether", RealmManager.Realm.SMP, false, false, WorldFlag.NETHER),
        SMP_END("smp_end", RealmManager.Realm.SMP, false, false, WorldFlag.THE_END),
        ANARCHY_WORLD("anarchy", RealmManager.Realm.ANARCHY, false, false, WorldFlag.OVERWORLD),
        ANARCHY_NETHER("anarchy_nether", RealmManager.Realm.ANARCHY, false, false, WorldFlag.NETHER),
        ANARCHY_END("anarchy_end", RealmManager.Realm.ANARCHY, false, false, WorldFlag.THE_END),

        CANVAS_WORLD("canvas", RealmManager.Realm.CANVAS, false, true, WorldFlag.VOID);

        private final String worldName;
        private final RealmManager.Realm realm;
        private final boolean preservedWorld, safeZone;
        private final WorldFlag flag;

        ServerWorld(String worldName, RealmManager.Realm realm, boolean preservedWorld, boolean safeZone, WorldFlag flag) {
            this.worldName = worldName;
            this.realm = realm;
            this.preservedWorld = preservedWorld;
            this.safeZone = safeZone;
            this.flag = flag;
        }

        private WorldCreator getWorldCreator() {
            WorldCreator worldCreator = new WorldCreator(worldName);

            World.Environment environment = World.Environment.NORMAL;

            switch(getWorldFlag()) {
                case NETHER:
                    environment = World.Environment.NETHER;
                    break;
                case THE_END:
                    environment = World.Environment.THE_END;
                    break;
                case FLAT:
                    worldCreator.type(WorldType.FLAT);
                    worldCreator.generateStructures(false);
                    break;
                case VOID:
                    worldCreator.generator(new ChunkGenerator() {
                        @Override
                        @SuppressWarnings({"NullableProblems", "deprecation"})
                        public ChunkData generateChunkData(World world, Random random, int x, int z, BiomeGrid biome) {
                            return createChunkData(world);
                        }
                    });
                    break;
            }

            worldCreator.environment(environment);

            return worldCreator;
        }

        public World getWorld() {
            return NoxetServer.getPlugin().getServer().createWorld(getWorldCreator());
        }

        public boolean isPreserved() {
            return preservedWorld && shouldAllowWorldPreservation;
        }

        public boolean isSafeZone() {
            return safeZone;
        }

        public WorldFlag getWorldFlag() {
            return flag;
        }

        public RealmManager.Realm getRealm() {
            return realm;
        }
    }

    @Override
    public void onEnable() {
        plugin = this;

        getServer().getPluginManager().registerEvents(new Events(), this);

        Motd.loadQuotes();

        logInfo("Loading commands...");

        CommandRegistration.registerCommands();

        logInfo("Noxet plugin is ready.");
    }

    @Override
    public void onDisable() {
        logInfo("Noxet plugin stopped.");
    }

    public static NoxetServer getPlugin() {
        return plugin;
    }

    public File getPluginDirectory() {
        File dir = getDataFolder();

        if(!dir.mkdir() && (!dir.exists() || !dir.isDirectory()))
            throw new RuntimeException("Cannot create plugin directory.");

        return dir;
    }
    
    public static void logInfo(String message) {
        getPlugin().getLogger().info(message);
    }

    public static void logWarning(String message) {
        getPlugin().getLogger().warning(message);
    }

    public static void logSevere(String message) {
        getPlugin().getLogger().severe(message);
    }

    public static boolean isWorldPreserved(World world) {
        for(ServerWorld serverWorld : ServerWorld.values())
            if(serverWorld.getWorld() == world)
                return serverWorld.isPreserved();
        return false;
    }

    public static boolean isWorldSafeZone(World world) {
        for(ServerWorld serverWorld : ServerWorld.values())
            if(serverWorld.getWorld() == world)
                return serverWorld.isSafeZone();
        return false;
    }
}
