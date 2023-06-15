package org.noxet.noxetserver.util;

import org.bukkit.Location;
import org.bukkit.Material;

import java.util.Arrays;
import java.util.List;

public class TeleportUtil {
    private static final List<Material> blacklistedMaterials = Arrays.asList(
            Material.LAVA,
            Material.FIRE,
            Material.NETHER_PORTAL,
            Material.CACTUS,
            Material.POWDER_SNOW,
            Material.MAGMA_BLOCK
    );

    public static boolean isLocationTeleportSafe(Location location) {
        Location below = location.clone().subtract(0, 1, 0),
                above = location.clone().add(0, 1, 0);

        List<Location> materialCheckLocations = Arrays.asList(
                location,
                below,
                above
        );

        for(Location materialCheckLocation : materialCheckLocations)
            if(blacklistedMaterials.contains(materialCheckLocation.getBlock().getType()))
                return false;

        return below.getBlock().getType().isSolid() &&
                !location.getBlock().getType().isSolid() &&
                !above.getBlock().getType().isSolid();
    }

    private static final int safeTeleportSearchRadius = 20;

    public static Location getSafeTeleportLocation(Location originLocation) {
        /*// Search is done in a spiral.

        assert originLocation.getWorld() != null;

        Location searchLocation = originLocation.clone();

        int searchLayer = 0, // Amount of layers from relative 0;0 (location search origin).
            currentSide = 0, // 0-3: what side of the rect to search at now.
            sideIndex = 0; // Where on the side?

        int attemptsLeft = (int) Math.pow(safeTeleportSearchRadius, 2);

        while(!isLocationTeleportSafe(searchLocation)) {
            if(attemptsLeft-- == 0)
                return null;

            if(sideIndex == searchLayer * 2 + 1 && currentSide++ == 3) { // Past last side and index, move to next layer.
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

        return searchLocation;*/
        assert originLocation.getWorld() != null;

        Location searchLocation = originLocation.clone();

        int searchLayer = 0, x = 0, z = 0, dx = 0, dz = -1;

        for(int i = 0; i <= safeTeleportSearchRadius * safeTeleportSearchRadius; i++) {
            if(-safeTeleportSearchRadius <= x && x <= safeTeleportSearchRadius && -safeTeleportSearchRadius <= z && z <= safeTeleportSearchRadius) {
                searchLocation.setX(originLocation.getX() + x);
                searchLocation.setY(originLocation.getY());
                searchLocation.setZ(originLocation.getZ() + z);

                while(searchLocation.clone().subtract(0, 1, 0).getBlock().isEmpty() && searchLocation.getY() > originLocation.getWorld().getMinHeight())
                    searchLocation.subtract(0, 1, 0);

                if(isLocationTeleportSafe(searchLocation))
                    return searchLocation;
            }

            if(x == z || (x < 0 && x == -z) || (x > 0 && x == 1 - z)) {
                int temp = dx;
                dx = -dz;
                dz = temp;
            }

            x += dx;
            z += dz;

            if(Math.abs(x) > searchLayer || Math.abs(z) > searchLayer) {
                int temp = dx;
                dx = -dz;
                dz = temp;

                if(x >= 0)
                    searchLayer++;
            }
        }

        return null; // No safe location found within the search radius
    }
}
