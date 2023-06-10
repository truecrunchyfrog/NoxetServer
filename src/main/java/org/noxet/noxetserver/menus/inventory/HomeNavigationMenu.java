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
import org.noxet.noxetserver.util.InventoryCoordinate;
import org.noxet.noxetserver.util.InventoryCoordinateUtil;

import java.text.DecimalFormat;
import java.util.*;

public class HomeNavigationMenu extends InventoryMenu {
    private final Player player;
    private final Map<String, Location> homes;

    public HomeNavigationMenu(Player player, Map<String, Location> homes) {
        super((homes.size() - 1) / 9 + 1, "⚑ Homes", false);
        this.homes = homes;
        this.player = player;
    }

    @Override
    protected void updateInventory() {
        int i = 0;

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
                // Same world: show distance.
                int blocksAway = (int) player.getLocation().distance(homeLocation);
                locationNote += " §e" + new DecimalFormat("#,###").format(blocksAway) + " blocks away.";
            }

            boolean isHomeMain = home.getKey().equals(Home.defaultHomeName);

            ItemStack homeItem = ItemGenerator.generateItem(
                    !isHomeMain ? Material.WHITE_BANNER : Material.RED_BED,
                    (!isHomeMain ? "§a" : "§5◆ §3") + home.getKey(),
                    Arrays.asList(
                            locationNote,
                            "§8X §7" + ((int) homeLocation.getX()),
                            "§8Y §7" + ((int) homeLocation.getY()),
                            "§8Z §7" + ((int) homeLocation.getZ()),
                            "§e→ Double-click to §5§nteleport§e.",
                            "§e→ Right-click to §b§nrename§e.",
                            "§e→ Press any number on keyboard to §c§nremove§e this home."
                    )
            );

            if(!isHomeMain)
                generateBannerPatterns(homeItem, home.getValue().hashCode());

            setSlotItem(homeItem, InventoryCoordinateUtil.getCoordinateFromSlotIndex(i++));
        }
    }

    @Override
    protected boolean onSlotClick(Player player, InventoryCoordinate coordinate, ClickType clickType) {
        int i = 0;

        for(Map.Entry<String, Location> home : homes.entrySet())
            if(InventoryCoordinateUtil.getCoordinateFromSlotIndex(i++).isAt(coordinate)) {
                switch(clickType) {
                    case DOUBLE_CLICK:
                        player.performCommand("home tp " + home.getKey());
                        break;
                    case NUMBER_KEY:
                        new ConfirmationMenu("Delete home '" + home.getKey() + "'?", () -> {
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
                                        new NoxetErrorMessage(NoxetErrorMessage.ErrorType.ARGUMENT, "Bad name for home. Too long/short.").send(player);
                                        return;
                                    }

                                    player.performCommand("home rename " + home.getKey() + " " + newName);
                                    player.performCommand("home menu");
                                }
                        );
                        break;
                    default:
                        return false;
                }

                return true;
            }

        return false;
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
