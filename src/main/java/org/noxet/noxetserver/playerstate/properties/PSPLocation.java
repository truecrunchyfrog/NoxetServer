package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.noxet.noxetserver.NoxetServer;
import org.noxet.noxetserver.playerstate.PlayerStateProperty;

public class PSPLocation implements PlayerStateProperty<Location> {
    @Override
    public String getConfigName() {
        return "location";
    }

    @Override
    public Location getDefaultSerializedProperty() {
        return NoxetServer.ServerWorld.HUB.getWorld().getSpawnLocation();
    }

    @Override
    public Location getSerializedPropertyFromPlayer(Player player) {
        return player.getLocation();
    }

    @Override
    public void restoreProperty(Player player, Location location) {
        player.teleport(location);
    }
}
