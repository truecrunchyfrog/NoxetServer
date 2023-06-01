package org.noxet.noxetserver.menus.inventory;

import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.noxet.noxetserver.commands.teleportation.Home;
import org.noxet.noxetserver.menus.ItemGenerator;
import org.noxet.noxetserver.menus.chat.ChatPromptMenu;
import org.noxet.noxetserver.messaging.NoxetErrorMessage;

import java.text.DecimalFormat;
import java.util.*;

public class HomeNavigationMenu extends InventoryMenu {
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

            ItemStack homeItem = ItemGenerator.generateItem(
                    Material.WHITE_BANNER,
                    "§a" + home.getKey(),
                    Arrays.asList(
                            locationNote,
                            "§8X §7" + ((int) homeLocation.getX()),
                            "§8Y §7" + ((int) homeLocation.getY()),
                            "§8Z §7" + ((int) homeLocation.getZ()),
                            "§f- - -",
                            "§3Left-click to teleport.",
                            "§3Right-click to rename.",
                            "§3Press any number on keyboard to remove."
                    )
            );

            generateBannerPatterns(homeItem, home.getValue().hashCode());

            setSlotItem(homeItem, x, y);

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
                switch(clickType) {
                    case NUMBER_KEY:
                        new ConfirmMenu("Delete home '" + home.getKey() + "'?", () -> {
                            player.performCommand("home remove " + home.getKey());
                            player.performCommand("home menu");
                        }, () -> player.performCommand("home menu")).openInventory(player);
                        break;
                    case RIGHT:
                        new ChatPromptMenu(
                                "new name for home '" + home.getKey() + "'",
                                player,
                                promptResponse -> {
                                    String newName = promptResponse.getMessage();

                                    if(!Home.isHomeNameOk(newName)) {
                                        new NoxetErrorMessage("Bad name for home. Too long/short.").send(player);
                                        return;
                                    }

                                    promptResponse.getResponder().performCommand("home rename " + home.getKey() + " " + promptResponse.getMessage());
                                }
                        );
                        break;
                    default:
                        player.performCommand("home tp " + home.getKey());
                }

                break;
            }
    }

    private void generateBannerPatterns(ItemStack banner, int seed) {
        Random random = new Random(seed);

        List<Pattern> patterns = new ArrayList<>();

        for(int i = 0; i < 10; i++) {
            // Append random patterns to the banner.
            // (based on location seed)
            Pattern pattern = new Pattern(
                    DyeColor.values()[random.nextInt(DyeColor.values().length)],
                    PatternType.values()[random.nextInt(PatternType.values().length)]
                    );
            patterns.add(pattern);
        }

        BannerMeta bannerMeta = (BannerMeta) banner.getItemMeta();
        assert bannerMeta != null;

        bannerMeta.setPatterns(patterns);
        bannerMeta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS); // Hide banner pattern lore.

        banner.setItemMeta(bannerMeta);
    }
}
