package org.noxet.noxetserver.menus.inventory;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.noxet.noxetserver.RealmManager;
import org.noxet.noxetserver.messaging.TextBeautifier;
import org.noxet.noxetserver.util.InventoryCoordinate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.noxet.noxetserver.menus.ItemGenerator.generateItem;

public class GameNavigationMenu extends InventoryMenu {
    public GameNavigationMenu() {
        super(3, TextBeautifier.beautify("Noxet - Choose a game"), false);
    }

    public enum GameSlot implements InventoryCoordinate {
        ANARCHY_ISLAND(2, 1),
        WORLD_EATER(4, 1),
        SMP(6, 1);

        private final int x, y;

        GameSlot(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public int getX() {
            return x;
        }

        @Override
        public int getY() {
            return y;
        }
    }

    @Override
    protected void updateInventory() {
        setSlotItem(generateGameModeItem(
                "Anarchy Island",
                ChatColor.RED,
                Arrays.asList("Insane vanilla experience.", "No rules. Try to live", "in a destructive world."),
                RealmManager.Realm.ANARCHY.getPlayerCount(),
                Material.FLINT_AND_STEEL
        ), GameSlot.ANARCHY_ISLAND);

        setSlotItem(generateGameModeItem(
                "WorldEater",
                ChatColor.DARK_GREEN,
                Arrays.asList("Hide and seek in one chunk.", "Hiders win if survived for", "30 minutes."),
                0,
                Material.SPRUCE_LOG
        ), GameSlot.WORLD_EATER);

        setSlotItem(generateGameModeItem(
                "Smp",
                ChatColor.BLUE,
                Arrays.asList("The (somewhat) vanilla", "experience."),
                RealmManager.Realm.SMP.getPlayerCount(),
                Material.GRASS_BLOCK
        ), GameSlot.SMP);
    }

    private ItemStack generateGameModeItem(String name, ChatColor nameColor, List<String> description, int players, Material material) {
        List<String> formattedDescription = new ArrayList<>();

        formattedDescription.add("§9" + players + " in game.");

        for(String descriptionPart : description)
            formattedDescription.add("§7§o" + descriptionPart);

        return generateItem(
                material,
                nameColor + TextBeautifier.beautify(name, false),
                formattedDescription
        );
    }

    @Override
    protected boolean onSlotClick(Player player, InventoryCoordinate coordinate, ClickType clickType) {
        if(coordinate.isAt(GameSlot.ANARCHY_ISLAND)) {
            RealmManager.migrateToRealm(player, RealmManager.Realm.ANARCHY);
        } else if(coordinate.isAt(GameSlot.WORLD_EATER)) {
            player.performCommand("eatworld play");
        } else if(coordinate.isAt(GameSlot.SMP)) {
            RealmManager.migrateToRealm(player, RealmManager.Realm.SMP);
        } else return false;

        return true;
    }
}
