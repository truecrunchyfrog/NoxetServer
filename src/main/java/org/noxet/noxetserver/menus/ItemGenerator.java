package org.noxet.noxetserver.menus;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class ItemGenerator {
    public static ItemStack generateItem(Material material, int amount, String name, List<String> lore, boolean glow) {
        ItemStack itemStack = new ItemStack(material);

        ItemMeta itemMeta = itemStack.getItemMeta();

        assert itemMeta != null;
        itemMeta.setDisplayName(name);
        itemMeta.setLore(lore);

        if(glow)
            itemMeta.addEnchant(Enchantment.ARROW_DAMAGE, 1, true);

        itemStack.setItemMeta(itemMeta);

        if(glow)
            itemStack.removeEnchantment(Enchantment.ARROW_DAMAGE);

        itemStack.setAmount(amount);

        return itemStack;
    }

    public static ItemStack generateItem(Material material, String name, List<String> lore) {
        return generateItem(material, 1, name, lore, false);
    }

    public static ItemStack generateItem(Material material, String name) {
        return generateItem(material, name, null);
    }
}
