package org.noxet.noxetserver.inventory.menus;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.noxet.noxetserver.RealmManager;
import org.noxet.noxetserver.messaging.TextBeautifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.noxet.noxetserver.inventory.ItemGenerator.generateItem;

public class GameNavigationMenu extends Menu {
    public GameNavigationMenu() {
        super(3, TextBeautifier.beautify("Noxet - Choose a game"), false);
    }

    @Override
    protected void updateInventory() {
        setSlotItem(generateGameModeItem(
                "Anarchy Island",
                ChatColor.RED,
                Arrays.asList("Insane vanilla experience.", "No rules. Try to live", "in a destructive world."),
                RealmManager.Realm.ANARCHY.getPlayerCount(),
                Material.FLINT_AND_STEEL
        ), 2, 1);

        setSlotItem(generateGameModeItem(
                "WorldEater",
                ChatColor.DARK_GREEN,
                Arrays.asList("Hide and seek in one chunk.", "Hiders win if survived for", "30 minutes."),
                0,
                Material.SPRUCE_LOG
        ), 4, 1);

        setSlotItem(generateGameModeItem(
                "Smp",
                ChatColor.BLUE,
                Arrays.asList("The (somewhat) vanilla", "experience."),
                RealmManager.Realm.SMP.getPlayerCount(),
                Material.GRASS_BLOCK
        ), 6, 1);
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
    protected void onSlotClick(Player player, int x, int y) {
        if(y == 1) {
            switch(x) {
                case 2:
                    RealmManager.migrateToRealm(player, RealmManager.Realm.ANARCHY);
                    break;
                case 4:
                    player.performCommand("eatworld play");
                    break;
                case 6:
                    RealmManager.migrateToRealm(player, RealmManager.Realm.SMP);
                    break;
                default:
                    return;
            }

            stop();
        }
    }
}
