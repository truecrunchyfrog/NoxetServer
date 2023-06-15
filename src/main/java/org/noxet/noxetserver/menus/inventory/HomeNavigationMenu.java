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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.noxet.noxetserver.NoxetServer;
import org.noxet.noxetserver.RealmManager;
import org.noxet.noxetserver.UsernameStorageManager;
import org.noxet.noxetserver.commands.misc.Friend;
import org.noxet.noxetserver.commands.teleportation.Home;
import org.noxet.noxetserver.menus.ItemGenerator;
import org.noxet.noxetserver.menus.chat.ChatPromptMenu;
import org.noxet.noxetserver.playerdata.PlayerDataManager;
import org.noxet.noxetserver.util.InventoryCoordinate;
import org.noxet.noxetserver.util.InventoryCoordinateUtil;

import java.text.DecimalFormat;
import java.util.*;

public class HomeNavigationMenu extends InventoryMenu {
    private final Player player;
    private final RealmManager.Realm realm;
    private final Map<String, Location> homes;
    private final InventoryCoordinate toggleFriendsHomesSlot;
    private final boolean showFriendsHomes;
    private final List<String> friendsHomes;

    private final BukkitTask toggleFriendsHomesSlotUpdateTimer;
    private int friendSkullScrollIndex = 0;

    public HomeNavigationMenu(Player player, RealmManager.Realm realm) {
        super(
                (Home.getRealmHomes(player, realm).size() - 1) / 9 + 2 +
                ((boolean) new PlayerDataManager(player).get(PlayerDataManager.Attribute.SHOW_FRIEND_HOMES) ? (Home.getFriendsHomes(player, realm).size() - 1) / 9 : 0),
                "⚑ " + realm.getDisplayName() + " Homes",
                false
        );

        homes = Home.getRealmHomes(player, realm);
        this.player = player;
        this.realm = realm;

        showFriendsHomes = (boolean) new PlayerDataManager(player).get(PlayerDataManager.Attribute.SHOW_FRIEND_HOMES);

        toggleFriendsHomesSlot = InventoryCoordinateUtil.getCoordinateFromXY(0, getInventory().getSize() / 9 - 1);

        friendsHomes = showFriendsHomes ? Home.getFriendsHomes(player, realm) : null;

        List<String> friends = Friend.getFriendList(player.getUniqueId());

        toggleFriendsHomesSlotUpdateTimer = new BukkitRunnable() {
            @Override
            public void run() {
                if(friends.size() > 0)
                    friendSkullScrollIndex %= friends.size();

                setSlotItem(
                        ItemGenerator.generatePlayerSkull(
                                friends.size() > 0 ? NoxetServer.getPlugin().getServer().getOfflinePlayer(
                                        new UsernameStorageManager().getUUIDFromUsernameOrUUID(
                                                friends.get(friendSkullScrollIndex++)
                                        )
                                ) : player,
                                "§aToggle Friends' Homes",
                                Arrays.asList(
                                        showFriendsHomes ? "§a§l✔ SHOWN" : "§c§l❌ HIDDEN",
                                        "§7When enabled, your friends' shared",
                                        "§7homes will be shown in this list.",
                                        "§e→ Click to " + (showFriendsHomes ? "hide" : "show") + "."
                                )
                        ), toggleFriendsHomesSlot
                );
            }
        }.runTaskTimer(NoxetServer.getPlugin(), 0, 40);
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

            List<String> lore = new ArrayList<>(Arrays.asList(
                    locationNote,
                    "§8X §7" + ((int) homeLocation.getX()),
                    "§8Y §7" + ((int) homeLocation.getY()),
                    "§8Z §7" + ((int) homeLocation.getZ()),
                    "§e→ Double-click to §5§nteleport§e.",
                    "§e→ Right-click to §b§nrename§e.",
                    "§e→ Press any number on keyboard to §c§nremove§e this home.",
                    "§e→ Shift-right-click to §d§n" + (!Home.isHomeFriendShared(home.getKey()) ? "enable" : "disable") + " friend sharing§e."
            ));

            ItemStack homeItem = ItemGenerator.generateItem(
                    !isHomeMain ? Material.WHITE_BANNER : Material.RED_BED,
                    !Home.isHomeFriendShared(home.getKey()) ? (!isHomeMain ? "§a" : "§5◆ §3") + home.getKey() : "§5☮ §a" + home.getKey().substring(1),
                    lore
            );

            if(!isHomeMain)
                generateBannerPatterns(homeItem, home.getValue().hashCode());

            setSlotItem(homeItem, InventoryCoordinateUtil.getCoordinateFromSlotIndex(i++));
        }

        if(!showFriendsHomes)
            return;

        for(String friendTpId : friendsHomes) {
            // friendTpId is formatted as: player/home-name

            String playerName = friendTpId.substring(0, friendTpId.indexOf('/'));
            UUID playerUUID = new UsernameStorageManager().getUUIDFromUsernameOrUUID(playerName);

            setSlotItem(
                    ItemGenerator.generatePlayerSkull(NoxetServer.getPlugin().getServer().getOfflinePlayer(playerUUID),
                    "§3" + friendTpId,
                    Arrays.asList(
                            "§7This home is shared by §b" + playerName + "§7.",
                            "§e→ Double-click to §5§nteleport§e."
                    )), InventoryCoordinateUtil.getCoordinateFromSlotIndex(i++)
            );
        }
    }

    @Override
    protected boolean onSlotClick(Player player, InventoryCoordinate coordinate, ClickType clickType) {
        if(coordinate.isAt(toggleFriendsHomesSlot)) {
            new PlayerDataManager(player).toggleBoolean(PlayerDataManager.Attribute.SHOW_FRIEND_HOMES).save();
            new HomeNavigationMenu(player, realm).openInventory(player);
            return false;
        }

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
                            new HomeNavigationMenu(player, realm).openInventory(player);
                        }, () -> new HomeNavigationMenu(player, realm).openInventory(player)).openInventory(player);
                        break;
                    case RIGHT:
                        new ChatPromptMenu(
                                "new name for home '" + home.getKey() + "'",
                                player,
                                promptResponse -> {
                                    String newName = promptResponse.getMessage();

                                    player.performCommand("home rename " + home.getKey() + " " + newName);
                                    new HomeNavigationMenu(player, realm).openInventory(player);
                                }
                        );
                        break;
                    case SHIFT_RIGHT:
                        boolean isAlreadyFriendShared = Home.isHomeFriendShared(home.getKey());
                        new ConfirmationMenu((isAlreadyFriendShared ? "Disable" : "Enable") + " friend sharing for home '" + home.getKey() + "'?", () -> {
                            player.performCommand("home rename " + home.getKey() + " " + (isAlreadyFriendShared ? home.getKey().substring(1) : "*" + home.getKey()));
                            new HomeNavigationMenu(player, realm).openInventory(player);
                        }, () -> new HomeNavigationMenu(player, realm).openInventory(player)).openInventory(player);
                        break;
                    default:
                        return false;
                }

                return true;
            }

        if(!showFriendsHomes)
            return false;

        for(String friendTpId : friendsHomes)
            if(InventoryCoordinateUtil.getCoordinateFromSlotIndex(i++).isAt(coordinate)) {
                switch(clickType) {
                    case DOUBLE_CLICK:
                        player.performCommand("home friend-tp " + friendTpId);
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

    @Override
    protected void stop() {
        super.stop();

        toggleFriendsHomesSlotUpdateTimer.cancel();
    }
}
