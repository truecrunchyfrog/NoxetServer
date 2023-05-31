package org.noxet.noxetserver.inventory.menus;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.noxet.noxetserver.inventory.ItemGenerator;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

public class HomeNavigationMenu extends Menu {
    private final Player player;
    private final Map<String, Location> homes;
    private static final Material[] bannerTypes = {
            Material.BLACK_BANNER,
            Material.BLUE_BANNER,
            Material.BROWN_BANNER,
            Material.CYAN_BANNER,
            Material.GRAY_BANNER,
            Material.GREEN_BANNER,
            Material.LIME_BANNER,
            Material.MAGENTA_BANNER,
            Material.ORANGE_BANNER,
            Material.PINK_BANNER,
            Material.PURPLE_BANNER,
            Material.RED_BANNER,
            Material.WHITE_BANNER,
            Material.YELLOW_BANNER
    };

    public HomeNavigationMenu(Player player, Map<String, Location> homes) {
        super(homes.size() / 9 + 1, "Homes", false);
        this.homes = homes;
        this.player = player;
    }

    @Override
    protected void updateInventory() {
        int x = 0, y = 0;
        for(Map.Entry<String, Location> home : homes.entrySet()) {
            Location homeLocation = home.getValue();
            World.Environment dimension = Objects.requireNonNull(homeLocation.getWorld()).getEnvironment();
            String locationNote = "§5";

            switch(dimension) {
                case NORMAL:
                    locationNote += "Overworld";
                    break;
                case NETHER:
                    locationNote += "Nether";
                    break;
                case THE_END:
                    locationNote += "The End";
                    break;
                default:
                    locationNote += "???";
            }

            if(homeLocation.getWorld() == player.getWorld()) {
                // Same world, show distance.
                int blocksAway = (int) player.getLocation().distance(homeLocation);
                locationNote += " §e" + new DecimalFormat("#,###").format(blocksAway) + " blocks away.";
            }

            setSlotItem(ItemGenerator.generateItem(
                    bannerTypes[Math.abs(home.getValue().hashCode()) % bannerTypes.length],
                    "§a" + home.getKey(),
                    Arrays.asList(
                            locationNote,
                            "§8X §7" + ((int) homeLocation.getX()),
                            "§8Y §7" + ((int) homeLocation.getY()),
                            "§8Z §7" + ((int) homeLocation.getZ()),
                            "§f- - -",
                            "§3Click to teleport.",
                            "§cPress any number on keyboard to remove."
                            )
            ), x, y);

            x++;
            x %= 9;
            if(x == 0)
                y++;
        }
    }

    @Override
    protected void onSlotClick(Player player, int x, int y, ClickType clickType) {
        int slotIndex = y * 9 + x, i = 0;

        for(Map.Entry<String, Location> home : homes.entrySet())
            if(i++ == slotIndex) {
                if(clickType.isKeyboardClick()) {
                    player.openInventory(new ConfirmMenu("Delete home '" + home.getKey() + "'?", () -> {
                        player.performCommand("home remove " + home.getKey());
                        player.performCommand("home menu");
                    }, () -> player.performCommand("home menu")).getInventory());
                } else {
                    player.performCommand("home tp " + home.getKey());
                }
                break;
            }
    }
}
