package org.noxet.noxetserver.inventory.menus;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.noxet.noxetserver.NoxetServer;
import org.noxet.noxetserver.RealmManager;
import org.noxet.noxetserver.messaging.TextBeautifier;

import java.util.Arrays;

import static org.noxet.noxetserver.inventory.ItemGenerator.generateItem;

public class GameNavigationMenu extends Menu {
    public GameNavigationMenu() {
        super(3, TextBeautifier.beautify("Noxet - Choose a game"), false);
    }

    @Override
    protected void updateInventory() {
        setSlotItem(generateItem(
                Material.FLINT_AND_STEEL,
                "§c§lA" + TextBeautifier.beautify("narchy") + " I" + TextBeautifier.beautify("sland"),
                Arrays.asList("§7§oInsane vanilla experience.", "§7§oNo rules. Try to live", "§7§oin a destructive world.")
        ), 2, 1);

        setSlotItem(generateItem(
                Material.SPRUCE_LOG,
                "§6§lW" + TextBeautifier.beautify("orld") + "§2§lE" + TextBeautifier.beautify("ater"),
                Arrays.asList("§7§oHide and seek in one chunk.", "§7§oHiders win if survived for", "§7§o30 minutes.")
        ), 4, 1);

        setSlotItem(generateItem(
                Material.GRASS_BLOCK,
                "§9§lS" + TextBeautifier.beautify("mp"),
                Arrays.asList("§7§oThe (somewhat) vanilla", "§7§oexperience.")
        ), 6, 1);
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
