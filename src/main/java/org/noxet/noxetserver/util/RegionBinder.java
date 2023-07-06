package org.noxet.noxetserver.util;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.noxet.noxetserver.messaging.Message;

import java.util.Set;

public class RegionBinder extends DynamicTimer {
    private final Location center;
    private final Set<Player> playerSet;
    private final int width, tickFrequency;

    public RegionBinder(Location center, Set<Player> playerSet, int width, int tickFrequency) {
        this.center = center;
        this.playerSet = playerSet;
        this.width = width;
        this.tickFrequency = tickFrequency;

        touchTimer();
    }

    @Override
    public boolean isTimerNecessary() {
        return !playerSet.isEmpty();
    }

    @Override
    public int getTickFrequency() {
        return tickFrequency;
    }

    @Override
    public void timerCall() {
        for(Player player : playerSet) {
            Location mobile = player.getLocation();
            Location delta = center.clone().subtract(mobile);

            if(Math.abs(delta.getX()) > width) {
                mobile.setX(center.getX() + width * -Math.signum(delta.getX()));
            } else if(Math.abs(delta.getZ()) > width) {
                mobile.setZ(center.getZ() + width * -Math.signum(delta.getZ()));
            } else continue;

            player.teleport(mobile);
            new Message("§cNuh uh!").send(player);
        }
    }
}
