package org.noxet.noxetserver.util;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.checkerframework.common.value.qual.IntRange;
import org.noxet.noxetserver.messaging.Message;

public class RegionBinder extends DynamicTimer {
    private final World world;
    private final Location center;
    private final int width, tickFrequency;

    @IntRange(from = 1)
    public RegionBinder(Location center, int width, int tickFrequency) {
        this.world = center.getWorld();
        this.center = center;
        this.width = width;
        this.tickFrequency = tickFrequency;
    }

    @Override
    public boolean isTimerNecessary() {
        return world.getPlayers().size() > 0;
    }

    @Override
    public int getTickFrequency() {
        return tickFrequency;
    }

    @Override
    public void run() {
        for(Player player : world.getPlayers()) {
            Location mobile = player.getLocation();
            Location delta = center.subtract(mobile);

            if(Math.abs(delta.getX()) > width) {
                mobile.setX(center.getX() + width * Math.signum(delta.getX()));
            } else if(Math.abs(delta.getZ()) > width) {
                mobile.setZ(center.getZ() + width * Math.signum(delta.getZ()));
            } else continue;

            player.teleport(mobile);
            new Message("§cNuh uh!").send(player);
        }
    }
}
