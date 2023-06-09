package org.noxet.noxetserver.menus;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.noxet.noxetserver.messaging.TextBeautifier;

public class HubInventory {
    private static ItemStack gameNavigator, socialNavigator;

    public static ItemStack getGameNavigator() {
        return gameNavigator != null ?
                gameNavigator :
                (gameNavigator =
                        ItemGenerator.generateItem(
                                Material.CLOCK,
                                "§6▶ " + TextBeautifier.beautify("Browse our Games", false)
                        )
                );
    }

    public static ItemStack getSocialNavigator() {
        return socialNavigator != null ?
                socialNavigator :
                (socialNavigator =
                        ItemGenerator.generateItem(
                                Material.HEART_OF_THE_SEA,
                                "§d❤ " + TextBeautifier.beautify("Social", false)
                        )
                );
    }

    private static ItemStack[] generateInventory() {
        ItemStack[] contents = new ItemStack[36];

        contents[3] = getGameNavigator();
        contents[5] = getSocialNavigator();

        return contents;
    }

    public static void applyToPlayer(Player player) {
        player.getInventory().setContents(generateInventory());
    }
}
