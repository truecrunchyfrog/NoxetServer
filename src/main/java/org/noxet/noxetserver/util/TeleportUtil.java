package org.noxet.noxetserver.util;

import org.bukkit.Location;

public class TeleportUtil {
    public static boolean isLocationTeleportSafe(Location location) {
        return location.clone().subtract(0, 1, 0).getBlock().getType().isSolid();
    }

    private static final int safeTeleportSearchRadius = 20;

    public static Location getSafeTeleportLocation(Location originLocation) {
        // Search is done in a spiral.

        assert originLocation.getWorld() != null;

        Location searchLocation = originLocation.clone();

        int searchLayer = 0, // Amount of layers from relative 0;0 (location search origin).
            currentSide = 0, // 0-3: what side of the rect to search at now.
            sideIndex = 0; // Where on the side?

        int attemptsLeft = 500;

        while(!isLocationTeleportSafe(searchLocation)) {
            if(attemptsLeft-- == 0)
                return null;

            if(sideIndex == searchLayer * 2 + 1 && currentSide++ == 3) { // Past last index, move to another side.
                if(++searchLayer > safeTeleportSearchRadius)
                    return null; // No safe location found.
                currentSide = sideIndex = 0;
            }

            int x, z,
                indexMethodLTR = -(searchLayer * 2 + 1) / 2 + sideIndex,
                indexMethodRTL = (searchLayer * 2 + 1) / 2 - sideIndex;


            switch(currentSide) {
                case 0: // Up
                    x = indexMethodLTR;
                    z = -searchLayer;
                    break;
                case 1: // Right
                    x = searchLayer;
                    z = indexMethodLTR;
                    break;
                case 2: // Down
                    x = indexMethodRTL;
                    z = searchLayer;
                    break;
                case 3: // Left
                    x = -searchLayer;
                    z = indexMethodRTL;
                    break;
                default:
                    x = z = 0;
            }

            searchLocation.setX(originLocation.getX() + x);
            searchLocation.setY(originLocation.getY());
            searchLocation.setZ(originLocation.getZ() + z);

            while(searchLocation.getBlock().isEmpty() && searchLocation.getY() > originLocation.getWorld().getMinHeight())
                searchLocation.subtract(0, 1, 0);

            sideIndex++;
        }

        return searchLocation;
    }
}
