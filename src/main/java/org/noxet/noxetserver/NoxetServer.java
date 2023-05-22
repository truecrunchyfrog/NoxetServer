package org.noxet.noxetserver;

import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.plugin.java.JavaPlugin;
import org.noxet.noxetserver.commands.anarchy.Anarchy;
import org.noxet.noxetserver.commands.hub.Hub;
import org.noxet.noxetserver.commands.smp.SMP;
import org.noxet.noxetserver.commands.smp.Wild;
import org.noxet.noxetserver.commands.spawn.Spawn;

import java.io.File;
import java.util.Objects;

public final class NoxetServer extends JavaPlugin {

    private static NoxetServer plugin;

    public enum ServerWorld {
        HUB("hub", null),
        SMP_SPAWN("smp_spawn", RealmManager.Realm.SMP),
        SMP_WORLD("smp_world", RealmManager.Realm.SMP),
        ANARCHY_WORLD("anarchy", RealmManager.Realm.ANARCHY);

        private final String worldName;
        private final RealmManager.Realm realm;

        ServerWorld(String worldName, RealmManager.Realm realm) {
            this.worldName = worldName;
            this.realm = realm;
        }

        public World getWorld() {
            return NoxetServer.getPlugin().getServer().getWorld(worldName);
        }

        private void loadWorld() {
            NoxetServer.getPlugin().getServer().createWorld(new WorldCreator(worldName));
        }

        public RealmManager.Realm getRealm() {
            return realm;
        }
    }

    @Override
    public void onEnable() {
        plugin = this;

        getServer().getPluginManager().registerEvents(new Events(), this);

        logInfo("Loading worlds...");

        loadWorlds();

        logInfo("Loading commands...");

        Objects.requireNonNull(getCommand("smp")).setExecutor(new SMP());
        Objects.requireNonNull(getCommand("anarchy")).setExecutor(new Anarchy());
        Objects.requireNonNull(getCommand("hub")).setExecutor(new Hub());
        Objects.requireNonNull(getCommand("spawn")).setExecutor(new Spawn());

        Objects.requireNonNull(getCommand("wild")).setExecutor(new Wild());

        logInfo("Noxet plugin is ready!");
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
    
    private static String getLogPrefix() {
        return "[ NOXET ] ";
    }

    public static void logInfo(String message) {
        getPlugin().getLogger().info(getLogPrefix() + message);
    }

    public static void logWarning(String message) {
        getPlugin().getLogger().warning(getLogPrefix() + message);
    }

    public static void logSevere(String message) {
        getPlugin().getLogger().severe(getLogPrefix() + message);
    }

    public static void loadWorlds() {
        for(ServerWorld serverWorld : ServerWorld.values())
            serverWorld.loadWorld();
    }
}
