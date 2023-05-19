package org.noxet.noxetserver;

import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.noxet.noxetserver.commands.smp.SMP;

import java.io.File;
import java.util.Objects;

public final class NoxetServer extends JavaPlugin {

    private static NoxetServer plugin;

    private enum ServerWorld {
        SMP_SPAWN("smpspawn"),
        SMP_WORLD("smpworld");

        private final String worldName;

        ServerWorld(String name) {
            worldName = name;
        }
    }

    World smpSpawn, smpWorld;

    @Override
    public void onEnable() {
        plugin = this;

        smpSpawn = getServer().getWorld(ServerWorld.SMP_SPAWN.worldName);
        smpWorld = getServer().getWorld(ServerWorld.SMP_WORLD.worldName);

        Objects.requireNonNull(getCommand("smp")).setExecutor(new SMP());

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
