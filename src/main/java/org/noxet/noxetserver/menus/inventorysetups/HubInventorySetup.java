package org.noxet.noxetserver.menus.inventorysetups;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.noxet.noxetserver.menus.ItemGenerator;
import org.noxet.noxetserver.util.TextBeautifier;

public class HubInventorySetup extends InventorySetup {
    public static final ItemStack

            gameNavigator = ItemGenerator.generateItem(
                Material.CLOCK,
                "§6▶ " + TextBeautifier.beautify("Browse our Games", false)
            ),

            socialNavigator = ItemGenerator.generateItem(
                Material.HEART_OF_THE_SEA,
                "§d❤ " + TextBeautifier.beautify("Social", false)
            ),

            settings = ItemGenerator.generateItem(
                Material.OBSERVER,
                "§3⚑ " + TextBeautifier.beautify("Settings", false)
            );

    @Override
    protected void populateInventory() {
        inventory.setItem(2, gameNavigator);
        inventory.setItem(4, socialNavigator);
        inventory.setItem(6, settings);
    }
}
