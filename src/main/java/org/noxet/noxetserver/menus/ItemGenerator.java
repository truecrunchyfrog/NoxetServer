package org.noxet.noxetserver.menus;

import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;

public class ItemGenerator {
    public static ItemStack generateItem(Material material, int amount, String name, List<String> lore) {
        ItemStack itemStack = new ItemStack(material);

        ItemMeta itemMeta = itemStack.getItemMeta();

        assert itemMeta != null;
        itemMeta.setDisplayName(name);
        itemMeta.setLore(lore);
        itemMeta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);

        itemStack.setItemMeta(itemMeta);

        itemStack.setAmount(amount);

        return itemStack;
    }

    public static ItemStack generateItem(Material material, String name, List<String> lore) {
        return generateItem(material, 1, name, lore);
    }

    public static ItemStack generateItem(Material material, String name) {
        return generateItem(material, name, null);
    }

    public static ItemStack generatePlayerSkull(OfflinePlayer skullPlayer, String name, List<String> lore) {
        ItemStack itemStack = new ItemStack(Material.PLAYER_HEAD);

        SkullMeta skullMeta = (SkullMeta) itemStack.getItemMeta();

        assert skullMeta != null;
        skullMeta.setOwningPlayer(skullPlayer);
        skullMeta.setDisplayName(name);
        skullMeta.setLore(lore);

        itemStack.setItemMeta(skullMeta);

        return itemStack;
    }
}
