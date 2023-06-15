package org.noxet.noxetserver.menus.inventory;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.noxet.noxetserver.creepersweeper.CreeperSweeperGame;
import org.noxet.noxetserver.creepersweeper.CreeperSweeperTile;
import org.noxet.noxetserver.menus.ItemGenerator;
import org.noxet.noxetserver.messaging.NoxetMessage;
import org.noxet.noxetserver.util.FancyTimeConverter;
import org.noxet.noxetserver.util.InventoryCoordinate;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class CreeperSweeperGameMenu extends InventoryMenu {
    private static final List<Material> countHintColors = Arrays.asList(
            Material.LIGHT_BLUE_DYE, // 1
            Material.GREEN_DYE,      // 2
            Material.RED_DYE,        // 3
            Material.BLUE_DYE,       // 4
            Material.ORANGE_DYE,     // 5
            Material.CYAN_DYE,       // 6
            Material.BLACK_DYE,      // 7
            Material.GRAY_DYE,       // 8
            Material.PURPLE_DYE      // 9
    );

    private final CreeperSweeperGame game;
    private final int height, creepers;

    public CreeperSweeperGameMenu(int height, int creepers) {
        super(height, "Creeper Sweeper (9x" + height + ", " + creepers + " creepers)", false);

        this.height = height;
        this.creepers = creepers;
        game = new CreeperSweeperGame(9, height, creepers);
    }

    @Override
    protected void updateInventory() {
        Iterator<Map.Entry<InventoryCoordinate, CreeperSweeperTile>> tileEntries = game.tileIterator();

        while(tileEntries.hasNext()) {
            Map.Entry<InventoryCoordinate, CreeperSweeperTile> tileEntry = tileEntries.next();
            CreeperSweeperTile tile = tileEntry.getValue();

            // Now, draw the tile:

            ItemStack tileItemStack;

            if(!tile.isRevealed() && !game.hasEnded()) {
                // Covered tile.
                tileItemStack = !tile.isFlagged() ?
                        ItemGenerator.generateItem(Material.GRASS_BLOCK, "§r") :
                        ItemGenerator.generateItem(Material.CREEPER_BANNER_PATTERN, "§c⚐");
            } else if(tile.isCreeperTile()) {
                // Uncovered creeper tile.
                tileItemStack = ItemGenerator.generateItem(Material.CREEPER_HEAD, "§c§lCREEPER!");
            } else if(tile.countCreeperNeighbors(game, tileEntry.getKey()) > 0) {
                // Uncovered safe tile (with creeper proximity number).
                int count = tile.countCreeperNeighbors(game, tileEntry.getKey());

                tileItemStack = ItemGenerator.generateItem(
                        countHintColors.get(count - 1),
                        count,
                        "§r",
                        null
                );
            } else
                tileItemStack = ItemGenerator.generateItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE, "§r");

            setSlotItem(tileItemStack, tileEntry.getKey());
        }
    }

    @Override
    protected boolean onSlotClick(Player player, InventoryCoordinate coordinate, ClickType clickType) {
        CreeperSweeperTile clickedTile = game.getTileAt(coordinate);

        if(clickedTile == null)
            return false;

        if(game.hasEnded()) {
            new CreeperSweeperGameMenu(height, creepers).openInventory(player);
            return true;
        }

        switch(clickType) {
            case LEFT: // Open tile.
                game.revealTileAt(coordinate);
                break;
            case RIGHT: // Flag tile.
                clickedTile.setFlagged(!clickedTile.isFlagged());
                break;
        }

        if(game.hasEnded()) {
            if(game.didWin()) {
                player.playSound(player, Sound.ENTITY_CREEPER_DEATH, 1, 0.5f);

                new NoxetMessage("§eYou beat Creeper Sweeper in §c" + FancyTimeConverter.deltaSecondsToFancyTime((int) (game.getGameDuration() / 1000)) + "§e.").send(player);
            } else {
                player.playSound(player, Sound.ENTITY_GENERIC_EXPLODE, 1, 1);
                new NoxetMessage("§c§lTss...! You revealed a creeper.").send(player);
            }
        }

        updateInventory();
        return false;
    }
}
