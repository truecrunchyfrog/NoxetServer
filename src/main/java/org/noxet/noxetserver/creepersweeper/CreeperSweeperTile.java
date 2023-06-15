package org.noxet.noxetserver.creepersweeper;

import org.noxet.noxetserver.util.InventoryCoordinate;
import org.noxet.noxetserver.util.InventoryCoordinateUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreeperSweeperTile {
    private boolean creeperTile, revealed, flagged;

    public CreeperSweeperTile() {
        creeperTile = revealed = flagged = false;
    }

    public boolean isRevealed() {
        return revealed;
    }

    /**
     * Reveals this tile. This will NOT reveal neighbor tiles, only this one!
     */
    public void reveal() {
        revealed = true;
    }

    /**
     * Reveals this tile, and neighbor tiles, recursively.
     */
    public void revealRecursively(CreeperSweeperGame game, InventoryCoordinate coordinate, List<InventoryCoordinate> ignoreList) {
        reveal();

        if(countCreeperNeighbors(game, coordinate) != 0)
            return;

        ignoreList.add(coordinate);

        for(Map.Entry<InventoryCoordinate, CreeperSweeperTile> neighborTileEntry : getNeighborTiles(game, coordinate).entrySet()) {
            boolean isIgnored = false;

            for(InventoryCoordinate ignoreCoordinate : ignoreList)
                if(neighborTileEntry.getKey().isAt(ignoreCoordinate)) {
                    isIgnored = true;
                    break;
                }

            if(!isIgnored)
                neighborTileEntry.getValue().revealRecursively(game, neighborTileEntry.getKey(), ignoreList);
        }
    }

    public boolean isCreeperTile() {
        return creeperTile;
    }

    public boolean makeCreeperTile() {
        if(creeperTile) // Already creeper tile.
            return false;
        return creeperTile = true;
    }

    public boolean isFlagged() {
        return flagged;
    }

    public void setFlagged(boolean flagged) {
        this.flagged = flagged;
    }

    public Map<InventoryCoordinate, CreeperSweeperTile> getNeighborTiles(CreeperSweeperGame game, InventoryCoordinate tileCoordinate) {
        Map<InventoryCoordinate, CreeperSweeperTile> tileList = new HashMap<>();

        for(int dx = 0; dx < 3; dx++)
            for(int dy = 0; dy < 3; dy++) {
                if(dx == 1 && dy == 1)
                    continue;

                int x = tileCoordinate.getX() - 1 + dx,
                    y = tileCoordinate.getY() - 1 + dy;

                if(x >= 9 || x < 0) // Only X is modulused' to we don't need to check Y for out of bounds.
                    continue;

                InventoryCoordinate coordinate = InventoryCoordinateUtil.getCoordinateFromXY(x, y);
                CreeperSweeperTile tile = game.getTileAt(coordinate);

                if(tile != null)
                    tileList.put(coordinate, tile);
            }

        return tileList;
    }

    public int countCreeperNeighbors(CreeperSweeperGame game, InventoryCoordinate tileCoordinate) {
        int creepers = 0;

        for(Map.Entry<InventoryCoordinate, CreeperSweeperTile> neighborTileEntry : getNeighborTiles(game, tileCoordinate).entrySet())
            if(neighborTileEntry.getValue().isCreeperTile())
                creepers++;

        return creepers;
    }
}
