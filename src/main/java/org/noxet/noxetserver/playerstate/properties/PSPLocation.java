package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.noxet.noxetserver.NoxetServer;

public class PSPLocation extends _PlayerStateProperty {
    @Override
    public String getConfigName() {
        return "location";
    }

    @Override
    public Object getDefaultSerializedProperty() {
        return NoxetServer.ServerWorld.HUB.getWorld().getSpawnLocation();
    }

    @Override
    public Object getSerializedPropertyFromPlayer(Player player) {
        return player.getLocation();
    }

    @Override
    public void restoreProperty(Player player, Object value) {
        player.teleport((Location) value);
    }
}
