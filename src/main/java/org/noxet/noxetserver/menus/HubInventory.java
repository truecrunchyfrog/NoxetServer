package org.noxet.noxetserver.menus;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.noxet.noxetserver.messaging.TextBeautifier;

public class HubInventory {
    private static ItemStack gameNavigator = null;

    public static ItemStack getGameNavigator() {
        if(gameNavigator == null)
            gameNavigator = ItemGenerator.generateItem(Material.CLOCK, "§6Browse §b" + TextBeautifier.beautify("noxet") + "§6 Games");

        return gameNavigator;
    }

    private static ItemStack[] generateInventory() {
        ItemStack[] contents = new ItemStack[36];

        contents[1] = getGameNavigator();

        return contents;
    }

    public static void applyToPlayer(Player player) {
        player.getInventory().setContents(generateInventory());
    }
}
