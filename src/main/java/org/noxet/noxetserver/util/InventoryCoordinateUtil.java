package org.noxet.noxetserver.util;

public class InventoryCoordinateUtil {
    public static InventoryCoordinate getCoordinateFromXY(int x, int y) {
        return new InventoryCoordinate() {
            @Override
            public int getX() {
                return x;
            }

            @Override
            public int getY() {
                return y;
            }
        };
    }

    public static InventoryCoordinate getCoordinateFromSlotIndex(int slotIndex) {
        return getCoordinateFromXY(slotIndex % 9, slotIndex / 9);
    }
}
