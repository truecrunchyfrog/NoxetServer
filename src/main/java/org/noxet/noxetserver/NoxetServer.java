package org.noxet.noxetserver;

import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.plugin.java.JavaPlugin;
import org.noxet.noxetserver.commands.anarchy.Anarchy;
import org.noxet.noxetserver.commands.hub.Hub;
import org.noxet.noxetserver.commands.misc.*;
import org.noxet.noxetserver.commands.smp.SMP;
import org.noxet.noxetserver.commands.smp.Wild;
import org.noxet.noxetserver.commands.spawn.Spawn;
import org.noxet.noxetserver.commands.teleportation.Home;
import org.noxet.noxetserver.commands.teleportation.TeleportAsk;
import org.noxet.noxetserver.messaging.Motd;

import java.io.File;
import java.util.Objects;

public final class NoxetServer extends JavaPlugin {

    private static NoxetServer plugin;

    public enum WorldFlag {
        NEUTRAL, OVERWORLD, NETHER, END
    }

    public static boolean shouldAllowWorldPreservation = true;

    public enum ServerWorld {
        HUB("hub", null, true, true, WorldFlag.NEUTRAL),
        SMP_SPAWN("smp_spawn", RealmManager.Realm.SMP, true, true, WorldFlag.NEUTRAL),
        SMP_WORLD("smp_world", RealmManager.Realm.SMP, false, false, WorldFlag.OVERWORLD),
        SMP_NETHER("smp_nether", RealmManager.Realm.SMP, false, false, WorldFlag.NETHER),
        SMP_END("smp_end", RealmManager.Realm.SMP, false, false, WorldFlag.END),
        ANARCHY_WORLD("anarchy", RealmManager.Realm.ANARCHY, false, false, WorldFlag.OVERWORLD),
        ANARCHY_NETHER("anarchy_nether", RealmManager.Realm.ANARCHY, false, false, WorldFlag.NETHER),
        ANARCHY_END("anarchy_end", RealmManager.Realm.ANARCHY, false, false, WorldFlag.END),

        CANVAS_WORLD("canvas", RealmManager.Realm.CANVAS, false, false, WorldFlag.NEUTRAL);

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
                case END:
                    environment = World.Environment.THE_END;
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

        Objects.requireNonNull(getCommand("smp")).setExecutor(new SMP());
        Objects.requireNonNull(getCommand("anarchy")).setExecutor(new Anarchy());
        Objects.requireNonNull(getCommand("hub")).setExecutor(new Hub());
        Objects.requireNonNull(getCommand("spawn")).setExecutor(new Spawn());
        Objects.requireNonNull(getCommand("canvas-world")).setExecutor(new CanvasWorld());

        Objects.requireNonNull(getCommand("wild")).setExecutor(new Wild());

        Objects.requireNonNull(getCommand("tpa")).setExecutor(new TeleportAsk());

        Objects.requireNonNull(getCommand("whereami")).setExecutor(new WhereAmI());

        Objects.requireNonNull(getCommand("games")).setExecutor(new GameSelector());

        Objects.requireNonNull(getCommand("home")).setExecutor(new Home());

        Objects.requireNonNull(getCommand("chickenleg")).setExecutor(new ChickenLeg());

        Objects.requireNonNull(getCommand("doas")).setExecutor(new DoAs());

        Objects.requireNonNull(getCommand("mute")).setExecutor(new Mute());
        Objects.requireNonNull(getCommand("unmute")).setExecutor(new Unmute());

        Objects.requireNonNull(getCommand("msg")).setExecutor(new MsgConversation());

        Objects.requireNonNull(getCommand("toggle-preserve")).setExecutor(new TogglePreserve());

        Objects.requireNonNull(getCommand("loop")).setExecutor(new Loop());

        Objects.requireNonNull(getCommand("set-realm-spawn")).setExecutor(new SetRealmSpawn());
        Objects.requireNonNull(getCommand("reset-realm-spawn")).setExecutor(new ResetRealmSpawn());

        Objects.requireNonNull(getCommand("friend")).setExecutor(new Friend());
        Objects.requireNonNull(getCommand("block")).setExecutor(new Block());
        Objects.requireNonNull(getCommand("unblock")).setExecutor(new Unblock());
        Objects.requireNonNull(getCommand("block-list")).setExecutor(new BlockList());

        Objects.requireNonNull(getCommand("clear-player-data-cache")).setExecutor(new ClearPlayerDataCache());

        Objects.requireNonNull(getCommand("fake-combat-log")).setExecutor(new FakeCombatLog());

        Objects.requireNonNull(getCommand("creeper-sweeper")).setExecutor(new CreeperSweeper());

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
    
    @SuppressWarnings("SameReturnValue")
    private static String getLogPrefix() {
        return "[ NOXET ] ";
    }

    public static void logInfo(String message) {
        getPlugin().getLogger().info(getLogPrefix() + message);
    }

// --Commented out by Inspection START (2023-05-28 19:34):
//    public static void logWarning(String message) {
//        getPlugin().getLogger().warning(getLogPrefix() + message);
//    }
// --Commented out by Inspection STOP (2023-05-28 19:34)

// --Commented out by Inspection START (2023-05-28 19:34):
//    public static void logSevere(String message) {
//        getPlugin().getLogger().severe(getLogPrefix() + message);
//    }
// --Commented out by Inspection STOP (2023-05-28 19:34)

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
