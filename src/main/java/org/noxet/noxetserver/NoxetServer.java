package org.noxet.noxetserver;

import org.bukkit.plugin.java.JavaPlugin;

public final class NoxetServer extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("Noxet server plugin is up!");
    }

    @Override
    public void onDisable() {
    }
}
