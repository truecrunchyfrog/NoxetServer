package org.noxet.noxetserver.creepersweeper;

import org.noxet.noxetserver.util.InventoryCoordinate;
import org.noxet.noxetserver.util.InventoryCoordinateUtil;

import java.util.*;

public class CreeperSweeperGame {
    public enum CreeperSweeperStatus {
        NOT_STARTED,
        PLAYING,
        ENDED
    }

    private final int width, height, creepers;
    private final Map<InventoryCoordinate, CreeperSweeperTile> tiles = new HashMap<>();
    private CreeperSweeperStatus status = CreeperSweeperStatus.NOT_STARTED;
    private boolean hasWon = false;
    private long gameStart;

    public CreeperSweeperGame(int width, int height, int creepers) {
        this.width = width;
        this.height = height;
        this.creepers = creepers;

        for(int i = 0; i < width * height; i++)
            tiles.put(InventoryCoordinateUtil.getCoordinateFromSlotIndex(i), new CreeperSweeperTile()); // Placeholder tiles.
    }

    public void start(InventoryCoordinate startCoordinate) {
        if(status != CreeperSweeperStatus.NOT_STARTED)
            return;

        status = CreeperSweeperStatus.PLAYING;

        tiles.clear(); // Clear placeholder tiles.

        List<CreeperSweeperTile> nonCreeperTiles = new ArrayList<>();

        for(int i = 0; i < width * height; i++) {
            CreeperSweeperTile tile = new CreeperSweeperTile();
            InventoryCoordinate tileCoordinate = InventoryCoordinateUtil.getCoordinateFromSlotIndex(i);
            tiles.put(tileCoordinate, tile);
            if(!tileCoordinate.isAt(startCoordinate))
                nonCreeperTiles.add(tile);
        }

        assert creepers <= width * height - 1;
        Random random = new Random();

        int creepersLeft = creepers;

        while(creepersLeft > 0) {
            nonCreeperTiles.remove(random.nextInt(nonCreeperTiles.size())).makeCreeperTile();
            creepersLeft--;
        }

        gameStart = System.currentTimeMillis();
    }

    public void stop() {
        if(status == CreeperSweeperStatus.ENDED)
            return;

        status = CreeperSweeperStatus.ENDED;
    }

    public long getGameDuration() {
        return System.currentTimeMillis() - gameStart;
    }

    public CreeperSweeperTile getTileAt(InventoryCoordinate coordinate) {
        Iterator<Map.Entry<InventoryCoordinate, CreeperSweeperTile>> tileEntries = tileIterator();

        while(tileEntries.hasNext()) {
            Map.Entry<InventoryCoordinate, CreeperSweeperTile> tileEntry = tileEntries.next();
            if(tileEntry.getKey().isAt(coordinate))
                return tileEntry.getValue();
        }

        return null;
    }

    public Iterator<Map.Entry<InventoryCoordinate, CreeperSweeperTile>> tileIterator() {
        return tiles.entrySet().iterator();
    }

    public boolean isPlaying() {
        return status == CreeperSweeperStatus.PLAYING;
    }

    public boolean hasEnded() {
        return status == CreeperSweeperStatus.ENDED;
    }

    public boolean didWin() {
        return hasWon;
    }

    /**
     * Performs a recursive tile reveal.
     * @param coordinate The tile to reveal
     */
    public void revealTileAt(InventoryCoordinate coordinate) {
        if(!isPlaying())
            start(coordinate);

        CreeperSweeperTile tile = getTileAt(coordinate);

        if(tile.isFlagged())
            return; // Cannot reveal flagged tile!

        if(tile.isCreeperTile()) {
            onRevealCreeperTile(coordinate);
            return;
        }

        tile.revealRecursively(this, coordinate, new ArrayList<>());

        Iterator<Map.Entry<InventoryCoordinate, CreeperSweeperTile>> tileEntries = tileIterator();

        while(tileEntries.hasNext()) {
            Map.Entry<InventoryCoordinate, CreeperSweeperTile> tileEntry = tileEntries.next();
            CreeperSweeperTile checkTile = tileEntry.getValue();
            if(!checkTile.isCreeperTile() && !checkTile.isRevealed())
                return;
        }

        onFinish();
    }

    public void onRevealCreeperTile(InventoryCoordinate coordinate) {
        stop();
    }

    public void onFinish() {
        hasWon = true;
        stop();
    }
}
