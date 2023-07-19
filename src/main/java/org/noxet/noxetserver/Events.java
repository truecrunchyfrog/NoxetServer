package org.noxet.noxetserver;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.*;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.noxet.noxetserver.combatlogging.CombatLogging;
import org.noxet.noxetserver.combatlogging.CombatLoggingStorageManager;
import org.noxet.noxetserver.commands.misc.ChickenLeg;
import org.noxet.noxetserver.commands.social.MsgConversation;
import org.noxet.noxetserver.commands.teleportation.TeleportAsk;
import org.noxet.noxetserver.menus.HubInventory;
import org.noxet.noxetserver.menus.book.BookMenu;
import org.noxet.noxetserver.menus.inventory.GameNavigationMenu;
import org.noxet.noxetserver.menus.inventory.SocialMenu;
import org.noxet.noxetserver.messaging.*;
import org.noxet.noxetserver.minigames.MiniGameController;
import org.noxet.noxetserver.minigames.MiniGameManager;
import org.noxet.noxetserver.minigames.party.Party;
import org.noxet.noxetserver.playerdata.PlayerDataManager;
import org.noxet.noxetserver.realm.RealmManager;
import org.noxet.noxetserver.util.*;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

import java.util.*;

import static org.noxet.noxetserver.realm.RealmManager.*;

public class Events implements Listener {
    public enum TemporaryCommand {
        READ_BEFORE_CHAT("read-before-chat"),
        UNDERSTAND_CHAT("understand-chat"),
        CONFIRM_BED_SPAWN("confirm-bed-spawn"),
        UNDERSTAND_ANARCHY("understand-anarchy");

        private final String command;
        TemporaryCommand(String command) {
            this.command = command;
        }

        public boolean isMessageThisCommand(PlayerCommandPreprocessEvent e) {
            return e.getMessage().equalsIgnoreCase(getSlashCommand());
        }

        public String getRawCommand() {
            return command;
        }

        public String getSlashCommand() {
            return "/" + command;
        }
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent e) {
        if(CombatLogging.isCombatLogged(e.getPlayer())) {
            CombatLogging.triggerLocationDisband(e.getPlayer());
            new Message("§cYou teleported away while combat logged and was killed in penalty.").send(e.getPlayer());

            new BukkitRunnable() {
                @Override
                public void run() {
                    if(!e.getPlayer().isOnline())
                        return;

                    Realm fromRealm = getRealmFromWorld(e.getFrom().getWorld());
                    Realm toRealm = getCurrentRealm(e.getPlayer());

                    if(fromRealm == toRealm)
                        new CombatLoggingStorageManager().combatLogRejoin(e.getPlayer(), toRealm);
                }
            }.runTaskLater(NoxetServer.getPlugin(), 1);
        }

        if(e.getTo() != null && e.getFrom().getWorld() != e.getTo().getWorld() && e.getTo().getWorld() != null) { // Teleporting to another world.
            if(e.getTo().getWorld().getName().equals("world")) {
                goToHub(e.getPlayer());
                e.setCancelled(true);
                return;
            }

            Realm toRealm = getRealmFromWorld(e.getTo().getWorld());

            migrateToRealm(e.getPlayer(), toRealm); // Migrator will send the player to spawn or last location in realm.

            // Don't cancel the teleportation!
        }
    }

    private static final Map<Player, Long> playersTimePlayed = new HashMap<>();

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        PlayerDataEraser playerDataEraser = new PlayerDataEraser();

        if(playerDataEraser.cancelPlayerDataErasePlan(e.getPlayer().getUniqueId()))
            new NoteMessage("Planned data removal canceled!\nThe data for your Minecraft account on Noxet was requested to be deleted. You have now aborted this.").send(e.getPlayer());

        playerDataEraser.performDataErasureCheck(); // Check for passed data deletion requests when a player has joined.

        PlayerDataManager.clearCacheForUUID(e.getPlayer().getUniqueId());

        new Message("§3■ " + TextBeautifier.beautify("Absorb the Echoes: ", false) + "§b§l" + TextBeautifier.beautify("noxet.org") + "§3!").send(e.getPlayer());

        playersTimePlayed.put(e.getPlayer(), System.currentTimeMillis());

        PlayerDataManager playerDataManager = new PlayerDataManager(e.getPlayer());

        int timesJoined = (int) playerDataManager.get(PlayerDataManager.Attribute.TIMES_JOINED) + 1,
            secondsPlayed = (int) playerDataManager.get(PlayerDataManager.Attribute.SECONDS_PLAYED);

        if(secondsPlayed != 0)
            new Message("Your total playtime: §f" + FancyTimeConverter.deltaSecondsToFancyTime(secondsPlayed)).send(e.getPlayer());

        String fancyJoinAmount = String.valueOf(timesJoined);

        if((timesJoined % 100) < 11 || (timesJoined % 100) > 13)
            switch(timesJoined % 10) {
                case 1:
                    fancyJoinAmount += "st";
                    break;
                case 2:
                    fancyJoinAmount += "nd";
                    break;
                case 3:
                    fancyJoinAmount += "rd";
                    break;
                default:
                    fancyJoinAmount += "th";
            }
        else
            fancyJoinAmount += "th";

        new Message("§b" + e.getPlayer().getDisplayName() + "§3 hopped on §bNoxet.org§3 for the §b§n" + TextBeautifier.beautify(fancyJoinAmount) + "§3 time.").broadcast();
        e.setJoinMessage(null);

        playerDataManager.set(
                PlayerDataManager.Attribute.TIMES_JOINED,
                (int) playerDataManager.get(PlayerDataManager.Attribute.TIMES_JOINED) + 1
        );

        playerDataManager.save();

        if(!((boolean) playerDataManager.get(PlayerDataManager.Attribute.HAS_DONE_CAPTCHA))) {
            new Captcha(e.getPlayer()).init();
            return;
        }

        if(!e.getPlayer().getUniqueId().equals(new UsernameStorageManager().getUUIDFromUsernameOrUUID(e.getPlayer().getName())))
            new UsernameStorageManager().assignUUIDToUsername(e.getPlayer().getName(), e.getPlayer().getUniqueId()); // Correct username if changed (either entirely or just by different casing).

        Bukkit.getScheduler().scheduleSyncDelayedTask(NoxetServer.getPlugin(), () -> {
            Realm realm = getCurrentRealm(e.getPlayer());

            if(realm != null) {
                setTemporaryInvulnerability(e.getPlayer());
                new Message("§eYou are in §l" + TextBeautifier.beautify(realm.getDisplayName(), false) + "§e.").addButton("Leave", ChatColor.RED, "Go to lobby", "hub").send(e.getPlayer());
                e.getPlayer().sendTitle("§e§l" + TextBeautifier.beautify(realm.getDisplayName()), "§3Type §b/hub §3to leave this realm.", 0, 120, 10);
            } else
                goToHub(e.getPlayer()); // Make sure player is at spawn.
        }, 10);

        updatePlayerListName(e.getPlayer());

        new CombatLoggingStorageManager().combatLogRejoin(e.getPlayer(), getCurrentRealm(e.getPlayer()));

        int incomingFriendRequests = new PlayerDataManager(e.getPlayer()).getListSize(PlayerDataManager.Attribute.INCOMING_FRIEND_REQUESTS);

        if(incomingFriendRequests != 0) {
            new Message("§6⚐ Incoming friend requests: §c" + incomingFriendRequests)
                    .addButton("Review", ChatColor.GREEN, "See who wants to befriend you", "friend incoming")
                    .send(e.getPlayer());
        }
    }

    public static void updatePlayerListName(Player player) {
        Realm realm = getCurrentRealm(player);

        String playerListPrefix = (realm != null ? "§e" + TextBeautifier.beautify(realm.getDisplayName()) + "§r " : "") + "§7";

        player.setPlayerListName(playerListPrefix + player.getDisplayName());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();

        setPlayerMigrationStatus(player, false);
        Captcha.stopPlayerCaptcha(player);
        TeleportAsk.abortPlayerRelatedRequests(player);
        abortUnconfirmedPlayerRespawn(player);
        MsgConversation.clearActiveConversationModes(player);
        PlayerDataManager.clearCacheForUUID(player.getUniqueId());
        CombatLogging.triggerLocationDisband(player);
        Party.abandonPlayer(player);

        PlayerDataManager playerDataManager = new PlayerDataManager(player);

        Long timePlayed = playersTimePlayed.remove(player);

        if(timePlayed != null)
            playerDataManager.addInt(PlayerDataManager.Attribute.SECONDS_PLAYED, (int) ((System.currentTimeMillis() - timePlayed) / 1000));

        playerDataManager.set(PlayerDataManager.Attribute.LAST_PLAYED, System.currentTimeMillis() / 1000);

        playerDataManager.save();

        new Message("§f" + player.getDisplayName() + "§7 left Noxet.org.").broadcast();
        e.setQuitMessage(null);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player player = e.getEntity();

        CombatLogging.clearCombatLog(player);

        Realm realm = getCurrentRealm(player);

        String deathMessage = e.getDeathMessage();
        e.setDeathMessage(null);

        if(realm == null)
            return;

        setPlayerMigrationStatus(player, true);

        Bukkit.getScheduler().scheduleSyncDelayedTask(NoxetServer.getPlugin(), () -> {
            player.sendTitle("§4☠", "§cYou died...", 40, 60, 20);
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40 + 60 + 20, 10, true, false));

            int totalPlays = 10;
            for(int i = 0; i < totalPlays; i++) {
                int finalI = i;
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        player.playSound(player.getLocation(), Sound.ENTITY_CAMEL_DEATH, 1, 2 - 1.5f * ((float) finalI / totalPlays));
                        player.playSound(player.getLocation(), Sound.BLOCK_BELL_USE, 1 * ((float) finalI / totalPlays), 2 - 1.5f * ((float) finalI / totalPlays));
                    }
                }.runTaskLater(NoxetServer.getPlugin(), i * 5);
            }
        }, 2);

        new Message("§c" + deathMessage + ".").send(getCurrentRealm(player));
    }

    @EventHandler
    public void onPlayerSpawnLocation(PlayerSpawnLocationEvent e) {
        if(e.getSpawnLocation().getWorld() != null && RealmManager.getRealmFromWorld(e.getSpawnLocation().getWorld()) == null)
            e.setSpawnLocation(RealmManager.getMainSpawn());
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent e) {
        if(RealmManager.getCurrentRealm(e.getPlayer()) != null)
            setTemporaryInvulnerability(e.getPlayer());
        else if(MiniGameManager.isPlayerBusyInGame(e.getPlayer()))
            return;

        new BukkitRunnable() {
            @Override
            public void run() {
                setPlayerMigrationStatus(e.getPlayer(), true);
            }
        }.runTaskLater(NoxetServer.getPlugin(), 20);

        e.setRespawnLocation(getRespawnLocation(e.getPlayer()));
    }

    @EventHandler
    public void onPlayerAdvancementDone(PlayerAdvancementDoneEvent e) {
        e.getPlayer().getWorld().setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
    }

    @EventHandler
    public void onServerListPing(ServerListPingEvent e) {
        e.setMotd(Motd.generateMotd());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onAsyncPlayerChat(AsyncPlayerChatEvent e) {
        boolean shouldSend = !e.isCancelled();
        e.setCancelled(true);

        if(Captcha.isPlayerDoingCaptcha(e.getPlayer()))
            return;

        if(shouldSend) {
            PlayerDataManager playerDataManager = new PlayerDataManager(e.getPlayer());

            if(!(boolean) playerDataManager.get(PlayerDataManager.Attribute.SEEN_CHAT_NOTICE)) {
                new Message(
                        "§eHello, " + e.getPlayer().getName() + "!\n" +
                                "Please read a message from us before you can chat.\n"
                ).addButton(
                        "Read",
                        ChatColor.GREEN,
                        "Read a message from us to start talking",
                        TemporaryCommand.READ_BEFORE_CHAT.getRawCommand()
                ).send(e.getPlayer());
                return;
            }

            if((boolean) playerDataManager.get(PlayerDataManager.Attribute.MUTED)) {
                new ErrorMessage(ErrorMessage.ErrorType.COMMON, "You are muted, and cannot chat at the moment!").send(e.getPlayer());
                return;
            }

            Realm realm = getCurrentRealm(e.getPlayer());
            MiniGameController game = null;

            String channelName = null;

            if(realm != null) {
                channelName = realm.getDisplayName();
            } else if(NoxetServer.ServerWorld.HUB.getWorld().equals(e.getPlayer().getWorld())) {
                channelName = "Hub";
            } else {
                game = MiniGameManager.findPlayersOrSpectatorsGame(e.getPlayer());
                if(game != null && game.hasStarted())
                    channelName = game.isPlayer(e.getPlayer()) ? "Game" : "Game Spectator";
                else if(game != null)
                    game = null;
            }

            Message message = new Message(
                    (channelName != null ? "§7" + TextBeautifier.beautify(channelName) + "§8⏵ " : "") + "§3" + e.getPlayer().getDisplayName() + "§8→ §f" + e.getMessage());
            message.setPrefix(null);

            if(game != null)
                game.sendGameMessage(message); // In game? Send message to the game.
            else if(realm != null)
                message.send(realm); // In a realm? Send message to the realm.
            else
                message.send(e.getPlayer().getWorld()); // Not in a game or realm? Send only to the player's world.
        }
    }

    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent e) {
        if(Captcha.isPlayerDoingCaptcha(e.getPlayer())) {
            e.setCancelled(true);
        } else if(TemporaryCommand.CONFIRM_BED_SPAWN.isMessageThisCommand(e)) {
            e.setCancelled(true);

            if(!unconfirmedPlayerRespawns.containsKey(e.getPlayer())) {
                new ErrorMessage(ErrorMessage.ErrorType.COMMON, "You cannot do this now.").send(e.getPlayer());
                return;
            }

            Location newBedSpawn = unconfirmedPlayerRespawns.remove(e.getPlayer());
            e.getPlayer().setBedSpawnLocation(newBedSpawn);

            if(e.getPlayer().getBedSpawnLocation() != null && newBedSpawn.getBlock().getBlockData() instanceof Bed)
                new Message("§aYour respawn location has been updated.").send(e.getPlayer());
            else
                new ErrorMessage(ErrorMessage.ErrorType.COMMON, "Could not change your respawn location.").send(e.getPlayer());
        } else if(TemporaryCommand.UNDERSTAND_ANARCHY.isMessageThisCommand(e)) {
            PlayerDataManager playerDataManager = new PlayerDataManager(e.getPlayer());
            if(!(boolean) playerDataManager.get(PlayerDataManager.Attribute.HAS_UNDERSTOOD_ANARCHY)) {
                playerDataManager.set(PlayerDataManager.Attribute.HAS_UNDERSTOOD_ANARCHY, true).save();
                e.getPlayer().closeInventory();
                new Message("§aThank you for understanding. We will not prompt you that again.").send(e.getPlayer());
                e.setCancelled(true);
            }
        } else if(TemporaryCommand.READ_BEFORE_CHAT.isMessageThisCommand(e)) {
            PlayerDataManager playerDataManager = new PlayerDataManager(e.getPlayer());
            if(!(boolean) playerDataManager.get(PlayerDataManager.Attribute.SEEN_CHAT_NOTICE)) {
                new BookMenu(Collections.singletonList(
                        new ComponentBuilder(
                                "§8Welcome to the §3" + TextBeautifier.beautify("noxet") + "§8 chat.\n" +
                                    "You can §0/msg§8 players to talk privately.\n" +
                                    "Make sure that you follow our rules!\n\n")
                                .append(new ComponentBuilder("§2§l■ I have read and understood this.").event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, TemporaryCommand.UNDERSTAND_CHAT.getSlashCommand())).event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Close this warning"))).create()).create()
                )).openMenu(e.getPlayer());

                e.setCancelled(true);
            }
        } else if(TemporaryCommand.UNDERSTAND_CHAT.isMessageThisCommand(e)) {
            PlayerDataManager playerDataManager = new PlayerDataManager(e.getPlayer());
            if(!(boolean) playerDataManager.get(PlayerDataManager.Attribute.SEEN_CHAT_NOTICE)) {
                playerDataManager.set(PlayerDataManager.Attribute.SEEN_CHAT_NOTICE, true).save();
                new Message("§aYou can now chat!").send(e.getPlayer());
                e.getPlayer().closeInventory();

                e.setCancelled(true);
            }
        }
    }

    public static void boostPlayer(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, (float) (Math.random() * 1.5 + 0.5));

        Vector velocity = new Vector(0, 0.5, 0).add(player.getLocation().getDirection().multiply(4));

        player.setVelocity(velocity);

        new ActionBarMessage("§d↑ ↑ ↑ ↑").send(player);
    }

    @EventHandler
    public void onPlayerToggleFlight(PlayerToggleFlightEvent e) {
        if(e.isFlying() && e.getPlayer().getWorld() == NoxetServer.ServerWorld.HUB.getWorld() && e.getPlayer().getGameMode() == GameMode.SURVIVAL) {
            if(Math.abs(e.getPlayer().getVelocity().getY()) < 0.334 && e.getPlayer().getLocation().getY() < e.getPlayer().getWorld().getSpawnLocation().getY() + 40)
                boostPlayer(e.getPlayer());
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        if(NoxetServer.isWorldPreserved(e.getBlock().getWorld()))
            e.setCancelled(true);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        if(NoxetServer.isWorldPreserved(e.getBlock().getWorld()))
            e.setCancelled(true);
    }

    @EventHandler
    public void onBlockForm(BlockFormEvent e) {
        if(NoxetServer.isWorldPreserved(e.getBlock().getWorld()))
            e.setCancelled(true);
    }

    @EventHandler
    public void onBlockGrow(BlockGrowEvent e) {
        if(NoxetServer.isWorldPreserved(e.getBlock().getWorld()))
            e.setCancelled(true);
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent e) {
        if(NoxetServer.isWorldPreserved(e.getBlock().getWorld()))
            e.setCancelled(true);
    }

    @EventHandler
    public void onEntityInteract(EntityInteractEvent e) {
        if(NoxetServer.isWorldPreserved(e.getBlock().getWorld()))
            e.setCancelled(true);
    }

    @EventHandler
    public void onBlockFade(BlockFadeEvent e) {
        if(NoxetServer.isWorldPreserved(e.getBlock().getWorld()))
            e.setCancelled(true);
    }

    @EventHandler
    public void onBlockDamage(BlockDamageEvent e) {
        if(NoxetServer.isWorldPreserved(e.getBlock().getWorld()))
            e.setCancelled(true);
    }

    @EventHandler
    public void onBlockBurn(BlockBurnEvent e) {
        if(NoxetServer.isWorldPreserved(e.getBlock().getWorld()))
            e.setCancelled(true);
    }

    @EventHandler
    public void onBlockFertilize(BlockFertilizeEvent e) {
        if(NoxetServer.isWorldPreserved(e.getBlock().getWorld()))
            e.setCancelled(true);
    }

    @EventHandler
    public void onBlockIgnite(BlockIgniteEvent e) {
        if(NoxetServer.isWorldPreserved(e.getBlock().getWorld()))
            e.setCancelled(true);
    }

    @EventHandler
    public void onBlockSpread(BlockSpreadEvent e) {
        if(NoxetServer.isWorldPreserved(e.getBlock().getWorld()))
            e.setCancelled(true);
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent e) {
        if(NoxetServer.isWorldPreserved(e.getPlayer().getWorld()))
            e.setCancelled(true);
    }

    @EventHandler
    public void onEntityPickupItem(EntityPickupItemEvent e) {
        if(NoxetServer.isWorldPreserved(e.getItem().getWorld()))
            e.setCancelled(true);
    }

    @EventHandler
    public void onItemSpawn(ItemSpawnEvent e) {
        if(NoxetServer.isWorldPreserved(e.getLocation().getWorld()))
            e.setCancelled(true);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        if(e.getAction() == Action.PHYSICAL && e.getPlayer().getWorld() == NoxetServer.ServerWorld.HUB.getWorld()) {
            boostPlayer(e.getPlayer());
            return;
        }

        if(e.getItem() != null && e.getAction() != Action.PHYSICAL) {
            if(e.getItem().equals(HubInventory.getGameNavigator())) {
                new GameNavigationMenu().openInventory(e.getPlayer());
                e.setCancelled(true);
            } else if(e.getItem().equals(HubInventory.getSocialNavigator())) {
                new SocialMenu(e.getPlayer()).openInventory(e.getPlayer());
                e.setCancelled(true);
            }
        }

        if(e.getAction() == Action.RIGHT_CLICK_AIR && e.getMaterial() == Material.ENDER_CHEST) {
            Realm realm = RealmManager.getCurrentRealm(e.getPlayer());

            if(realm != null && realm.doesAllowTeleportationMethods())
                e.getPlayer().performCommand("enderchest");
        }

        if(NoxetServer.isWorldPreserved(e.getPlayer().getWorld()) && !(e.getAction() == Action.RIGHT_CLICK_BLOCK && e.getClickedBlock() != null && e.getClickedBlock().getType() == Material.ENDER_CHEST)) {
            e.setCancelled(true);
            return;
        }

        if(e.getAction() == Action.LEFT_CLICK_AIR && ChickenLeg.isPlayerChickenLeg(e.getPlayer()))
            ChickenLeg.summonChickenLeg(e.getPlayer());
    }



    @EventHandler
    public void onEntityChangeBlock(EntityChangeBlockEvent e) {
        if(NoxetServer.isWorldPreserved(e.getBlock().getWorld()))
            e.setCancelled(true);
    }

    private static final Set<Player> invulnerablePlayers = new HashSet<>();
    
    public static void setTemporaryInvulnerability(Player player, int secondsInvulnerable) {
        if(invulnerablePlayers.contains(player) || NoxetServer.isWorldSafeZone(player.getWorld()))
            return;

        invulnerablePlayers.add(player);

        int ticksInvulnerable = 20 * secondsInvulnerable;

        for(int i = ticksInvulnerable; i > 0; i -= 20) {
            int finalI = i;
            new BukkitRunnable() {
                @Override
                public void run() {
                    new ActionBarMessage("§e§lINVULNERABLE §c" + (finalI / 20) + "s").send(player);
                }
            }.runTaskLater(NoxetServer.getPlugin(), ticksInvulnerable - i);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                new ActionBarMessage("§cYou are no longer invulnerable.").send(player);
                invulnerablePlayers.remove(player);
            }
        }.runTaskLater(NoxetServer.getPlugin(), ticksInvulnerable);
    }

    public static void setTemporaryInvulnerability(Player player) {
        setTemporaryInvulnerability(player, 6);
    }

    private final Set<Player> recentlyDamageRespawnedPlayers = new HashSet<>();

    @EventHandler
    public void onEntityDamage(EntityDamageEvent e) {
        if(NoxetServer.isWorldSafeZone(e.getEntity().getWorld())) {
            if(
                    e.getEntity() instanceof Player &&
                    (
                            (e.getCause() == EntityDamageEvent.DamageCause.VOID && // Stuck in void
                            e.getEntity().getLocation().getY() < e.getEntity().getWorld().getMinHeight())
                            ||
                            e.getCause() == EntityDamageEvent.DamageCause.SUFFOCATION // Stuck in wall
                    ) &&
                    !recentlyDamageRespawnedPlayers.contains((Player) e.getEntity())
            ) {
                recentlyDamageRespawnedPlayers.add((Player) e.getEntity());
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        recentlyDamageRespawnedPlayers.remove((Player) e.getEntity());
                    }
                }.runTaskLater(NoxetServer.getPlugin(), 120);
                goToSpawn((Player) e.getEntity());
            }
            e.setCancelled(true);
        } else if(e.getEntity() instanceof Player && invulnerablePlayers.contains((Player) e.getEntity())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if(e.getWhoClicked().getWorld() == NoxetServer.ServerWorld.HUB.getWorld() || invulnerablePlayers.contains((Player) e.getWhoClicked()))
            e.setCancelled(true);
    }

    @EventHandler
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent e) {
        if(e.getPlayer().getWorld() == NoxetServer.ServerWorld.HUB.getWorld() || invulnerablePlayers.contains(e.getPlayer()))
            e.setCancelled(true);
    }

    @EventHandler
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent e) {
        if(invulnerablePlayers.contains(e.getPlayer()))
            e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        if(
                !e.isCancelled() &&
                e.getEntity() instanceof Player &&
                e.getDamager() instanceof Player
        ) {
            CombatLogging.triggerCombatLog((Player) e.getEntity());
            CombatLogging.triggerCombatLog((Player) e.getDamager());
        }
    }

    @EventHandler
    public void onEntityPortalEnter(EntityPortalEnterEvent e) {
        if(e.getEntity() instanceof Player && e.getLocation().getWorld() == NoxetServer.ServerWorld.HUB.getWorld()) {
            Player player = (Player) e.getEntity();

            goToSpawn(player);

            new BukkitRunnable() {
                @Override
                public void run() {
                    new GameNavigationMenu().openInventory(player);
                }
            }.runTaskLater(NoxetServer.getPlugin(), 5);
        }
    }

    @EventHandler
    public void onEntityPortal(EntityPortalEvent e) {
        if(MiniGameController.isGameWorld(e.getFrom().getWorld())) {
            e.setCancelled(true);
            return;
        }

        handlePortalTeleport(e.getFrom(), e.getTo());
    }

    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent e) {
        if(MiniGameController.isGameWorld(e.getFrom().getWorld())) {
            e.setCancelled(true);
            return;
        }

        handlePortalTeleport(e.getFrom(), e.getTo());
    }

    private void handlePortalTeleport(Location from, Location to) {
        World sourceWorld = from.getWorld();
        Realm realm = getRealmFromWorld(sourceWorld);

        if(realm == null)
            return; // No realm, we don't have to handle this.

        if(to == null)
            return;

        switch(Objects.requireNonNull(to.getWorld()).getEnvironment()) {
            case NORMAL: // To overworld
                to.setWorld(realm.getWorld(NoxetServer.WorldFlag.OVERWORLD));
                break;
            case NETHER: // To nether
                to.setWorld(realm.getWorld(NoxetServer.WorldFlag.NETHER));
                break;
            case THE_END: // To end
                to.setWorld(realm.getWorld(NoxetServer.WorldFlag.THE_END));
                break;
        }
    }

    private static final Map<Player, Location> unconfirmedPlayerRespawns = new HashMap<>();

    public static void abortUnconfirmedPlayerRespawn(Player player) {
        if(unconfirmedPlayerRespawns.remove(player) != null)
            new Message("§cYour respawn location was not changed.").send(player);
    }

    @EventHandler
    @SuppressWarnings("UnstableApiUsage")
    public void onPlayerSpawnChange(PlayerSpawnChangeEvent e) {
        Location oldBedSpawn = e.getPlayer().getBedSpawnLocation();

        if(oldBedSpawn != null && e.getNewSpawn() != null && oldBedSpawn.getBlock().getLocation().distance(e.getNewSpawn()) > 2 && e.getCause() == PlayerSpawnChangeEvent.Cause.BED) {
            unconfirmedPlayerRespawns.put(e.getPlayer(), e.getNewSpawn());

            new BukkitRunnable() {
                @Override
                public void run() {
                    new ActionBarMessage("§cYour spawn location was §lNOT§c changed! Read chat.").send(e.getPlayer());
                }
            }.runTaskLater(NoxetServer.getPlugin(), 0);

            new WarningMessage("You already have a respawn location. Replace it?")
                    .addButton("Replace", ChatColor.RED, "Set this as your new spawn", TemporaryCommand.CONFIRM_BED_SPAWN.getRawCommand())
                    .send(e.getPlayer());

            Realm realm = getCurrentRealm(e.getPlayer());

            if(realm != null && realm.doesAllowTeleportationMethods())
                new NoteMessage("In " + realm.getDisplayName() + ", you can save locations which you can teleport to, simply with the /home command.")
                        .addButton(
                                "Add home here",
                                ChatColor.GREEN,
                                "Add a home to easily get here",
                                "home set ?"
                        )
                        .send(e.getPlayer());

            new BukkitRunnable() {
                @Override
                public void run() {
                    unconfirmedPlayerRespawns.remove(e.getPlayer());
                }
            }.runTaskLater(NoxetServer.getPlugin(), 20 * 30);

            e.setCancelled(true);
        }
    }

    private static final Set<UUID> recentlyKickedToRemoveCheats = new HashSet<>();

    public static void setPlayerRecentlyKickedToRemoveCheats(UUID uuid) {
        recentlyKickedToRemoveCheats.add(uuid);

        new BukkitRunnable() {
            @Override
            public void run() {
                recentlyKickedToRemoveCheats.remove(uuid);
            }
        }.runTaskLater(NoxetServer.getPlugin(), 20 * 10);
    }

    @EventHandler
    public void onAsyncPayerPreLogin(AsyncPlayerPreLoginEvent e) {
        if(recentlyKickedToRemoveCheats.contains(e.getUniqueId()))
            e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, "§cThat was a little fast, wasn't it? Wait a few seconds.");
    }
}
