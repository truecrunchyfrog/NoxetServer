package org.noxet.noxetserver;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.noxet.noxetserver.commands.smp.Anarchy;
import org.noxet.noxetserver.commands.smp.SMP;

import java.io.File;
import java.util.Objects;

public final class NoxetServer extends JavaPlugin {

    private static NoxetServer plugin;

    public enum ServerWorld {
        HUB("hub", null),
        SMP_SPAWN("smp_spawn", RealmManager.Realm.SMP),
        SMP_WORLD("smp_world", RealmManager.Realm.SMP),
        ANARCHY_WORLD("anarchy", RealmManager.Realm.ANARCHY);

        private final World world;
        private final RealmManager.Realm realm;

        ServerWorld(String worldName, RealmManager.Realm realm) {
            this.world = NoxetServer.getPlugin().getServer().getWorld(worldName);
            this.realm = realm;
        }

        public World getWorld() {
            return world;
        }

        public RealmManager.Realm getRealm() {
            return realm;
        }
    }

    World smpSpawn, smpWorld;

    @Override
    public void onEnable() {
        plugin = this;

        Objects.requireNonNull(getCommand("smp")).setExecutor(new SMP());
        Objects.requireNonNull(getCommand("anarchy")).setExecutor(new Anarchy());

        getLogger().info("Noxet server plugin is up!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Noxet server plugin shut down.");
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
}
