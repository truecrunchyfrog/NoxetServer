package org.noxet.noxetserver;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
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
import org.noxet.noxetserver.commands.misc.ChickenLeg;
import org.noxet.noxetserver.commands.teleportation.TeleportAsk;
import org.noxet.noxetserver.menus.HubInventory;
import org.noxet.noxetserver.menus.book.BookMenu;
import org.noxet.noxetserver.menus.inventory.GameNavigationMenu;
import org.noxet.noxetserver.messaging.Motd;
import org.noxet.noxetserver.messaging.NoxetErrorMessage;
import org.noxet.noxetserver.messaging.NoxetMessage;
import org.noxet.noxetserver.messaging.TextBeautifier;
import org.noxet.noxetserver.playerdata.PlayerDataManager;

import java.util.*;

import static org.noxet.noxetserver.RealmManager.*;

public class Events implements Listener {
    public enum TemporaryCommand {
        READ_BEFORE_CHAT("read-before-chat"),
        UNDERSTAND_CHAT("understand-chat"),
        CONFIRM_BED_SPAWN("confirm-bed-spawn"),
        UNDERSTAND_ANARCHY("understand-anarchy");

        private String command;
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
        if(e.getTo() != null && e.getFrom().getWorld() != e.getTo().getWorld()) { // Teleporting to another world.
            RealmManager.Realm toRealm = getRealmFromWorld(e.getTo().getWorld());

            migrateToRealm(e.getPlayer(), toRealm); // Migrator will send the player to spawn or last location in realm.

            // Don't cancel the teleportation!
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        new NoxetMessage("§3§lWELCOME TO §b§lNOXET.ORG NETWORK§3§l!").send(e.getPlayer());
        new NoxetMessage("§f" + e.getPlayer().getDisplayName() + "§7 joined Noxet.org.").broadcast();
        e.setJoinMessage(null);

        PlayerDataManager playerDataManager = new PlayerDataManager(e.getPlayer());

        if(!((boolean) playerDataManager.get(PlayerDataManager.Attribute.HAS_DONE_CAPTCHA))) {
            new Captcha(e.getPlayer()).init();
            return;
        }

        Bukkit.getScheduler().scheduleSyncDelayedTask(NoxetServer.getPlugin(), () -> {
            Realm realm = getCurrentRealm(e.getPlayer());

            if(realm != null) {
                setTemporaryInvulnerability(e.getPlayer());
                new NoxetMessage("§eYou are in §6§l" + realm.getDisplayName() + "§e.").addButton("Leave", ChatColor.RED, "Go to lobby", "hub").send(e.getPlayer());
                e.getPlayer().sendTitle("§e§l" + realm.getDisplayName(), "§6Type §e§l/hub §6to leave this realm.", 0, 120, 10);
            } else
                goToHub(e.getPlayer()); // Make sure player is at spawn.
        }, 10);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        migratingPlayers.remove(e.getPlayer());
        Captcha.stopPlayerCaptcha(e.getPlayer());
        TeleportAsk.abortPlayerRelatedRequests(e.getPlayer());
        abortUnconfirmedPlayerRespawn(e.getPlayer());

        new NoxetMessage("§f" + e.getPlayer().getDisplayName() + "§7 left Noxet.org.").broadcast();
        e.setQuitMessage(null);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player player = e.getEntity();

        migratingPlayers.add(player);

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

        new NoxetMessage("§c" + e.getDeathMessage() + ".").send(getCurrentRealm(player));
        e.setDeathMessage(null);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent e) {
        setTemporaryInvulnerability(e.getPlayer());

        new BukkitRunnable() {
            @Override
            public void run() {
                migratingPlayers.remove(e.getPlayer());
            }
        }.runTaskLater(NoxetServer.getPlugin(), 20);

        e.setRespawnLocation(getRespawnLocation(e.getPlayer()));
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

        if(e.getPlayer().isOp() && e.getMessage().startsWith("!:")) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    String amountAndCommand = e.getMessage().substring(2);

                    if(amountAndCommand.indexOf(' ') == -1) {
                        new NoxetErrorMessage(NoxetErrorMessage.ErrorType.ARGUMENT, "Missing command.").send(e.getPlayer());
                        return;
                    }

                    int amount;

                    try {
                        amount = Integer.parseInt(amountAndCommand.substring(0, amountAndCommand.indexOf(' ')));
                    } catch(NumberFormatException e1) {
                        new NoxetErrorMessage(NoxetErrorMessage.ErrorType.ARGUMENT, "Syntax: '!:5 summon giant' to repeat command 'summon giant' 5 times.").send(e.getPlayer());
                        return;
                    }

                    if(amount < 1 || amount > 100) {
                        new NoxetErrorMessage(NoxetErrorMessage.ErrorType.ARGUMENT, "Amount must be within 1-100 (inclusive).").send(e.getPlayer());
                        return;
                    }

                    String command = amountAndCommand.substring(amountAndCommand.indexOf(' ') + 1);

                    for(int i = 0; i < amount; i++)
                        e.getPlayer().performCommand(command);
                }
            }.runTaskLater(NoxetServer.getPlugin(), 0);
            return;
        }


        if(shouldSend) {
            NoxetMessage message = new NoxetMessage(
                    "§7" + e.getPlayer().getDisplayName() + "§8→ §f" + e.getMessage());
            message.skipPrefix();
            Realm realm = getCurrentRealm(e.getPlayer());

            PlayerDataManager playerDataManager = new PlayerDataManager(e.getPlayer());

            if(!(boolean) playerDataManager.get(PlayerDataManager.Attribute.SEEN_CHAT_NOTICE)) {
                new NoxetMessage(
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
                new NoxetErrorMessage(NoxetErrorMessage.ErrorType.COMMON, "You are muted, and cannot chat at the moment!").send(e.getPlayer());
                return;
            }

            if(realm != null)
                message.send(realm);
            else
                message.send(NoxetServer.ServerWorld.HUB.getWorld());
        }
    }

    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent e) {
        if(Captcha.isPlayerDoingCaptcha(e.getPlayer())) {
            e.setCancelled(true);

            int answer;

            try {
                answer = Integer.parseInt(e.getMessage().substring(1));
            } catch(NumberFormatException numberFormatException) {
                return; // Not an integer.
            }

            if(answer < 0 || answer > Captcha.answersPerQuestion - 1)
                return;

            Objects.requireNonNull(Captcha.getPlayerCaptcha(e.getPlayer())).chooseAnswer(answer);
        } else if(TemporaryCommand.CONFIRM_BED_SPAWN.isMessageThisCommand(e)) {
            e.setCancelled(true);

            if(!unconfirmedPlayerRespawns.containsKey(e.getPlayer())) {
                new NoxetErrorMessage(NoxetErrorMessage.ErrorType.COMMON, "You cannot do this now.").send(e.getPlayer());
                return;
            }

            Location newBedSpawn = unconfirmedPlayerRespawns.remove(e.getPlayer());
            e.getPlayer().setBedSpawnLocation(newBedSpawn);

            if(e.getPlayer().getBedSpawnLocation() != null && newBedSpawn.getBlock().getBlockData() instanceof Bed)
                new NoxetMessage("§aYour respawn location has been updated.").send(e.getPlayer());
            else
                new NoxetErrorMessage(NoxetErrorMessage.ErrorType.COMMON, "Could not change your respawn location.").send(e.getPlayer());
        } else if(TemporaryCommand.UNDERSTAND_ANARCHY.isMessageThisCommand(e)) {
            PlayerDataManager playerDataManager = new PlayerDataManager(e.getPlayer());
            if(!(boolean) playerDataManager.get(PlayerDataManager.Attribute.HAS_UNDERSTOOD_ANARCHY)) {
                playerDataManager.set(PlayerDataManager.Attribute.HAS_UNDERSTOOD_ANARCHY, true).save();
                e.getPlayer().closeInventory();
                new NoxetMessage("§aThank you for understanding. We will not prompt you that again.").send(e.getPlayer());
                e.setCancelled(true);
            }
        } else if(TemporaryCommand.READ_BEFORE_CHAT.isMessageThisCommand(e)) {
            PlayerDataManager playerDataManager = new PlayerDataManager(e.getPlayer());
            if(!(boolean) playerDataManager.get(PlayerDataManager.Attribute.SEEN_CHAT_NOTICE)) {
                new BookMenu(Collections.singletonList(
                        new ComponentBuilder(
                                "§8Welcome to the §b" + TextBeautifier.beautify("noxet") + "§8 chat.\n" +
                                    "Each realm has its own chat channel.\n" +
                                    "You can still §l/msg§8 players outside of your realm.\n" +
                                    "Make sure that you follow our rules!\n\n")
                                .append(new ComponentBuilder("§2§l■ I have read and understood this.").event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, TemporaryCommand.UNDERSTAND_CHAT.getSlashCommand())).event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Close this warning"))).create()).create()
                )).openMenu(e.getPlayer());

                e.setCancelled(true);
            }
        } else if(TemporaryCommand.UNDERSTAND_CHAT.isMessageThisCommand(e)) {
            PlayerDataManager playerDataManager = new PlayerDataManager(e.getPlayer());
            if(!(boolean) playerDataManager.get(PlayerDataManager.Attribute.SEEN_CHAT_NOTICE)) {
                playerDataManager.set(PlayerDataManager.Attribute.SEEN_CHAT_NOTICE, true).save();
                new NoxetMessage("§aYou can now chat!").send(e.getPlayer());
                e.getPlayer().closeInventory();

                e.setCancelled(true);
            }
        }
    }

    private static final Set<Player> boostingCooldownedPlayers = new HashSet<>();

    @EventHandler
    public void onPlayerFly(PlayerToggleFlightEvent e) {
        if(e.isFlying() && e.getPlayer().getWorld() == NoxetServer.ServerWorld.HUB.getWorld() && e.getPlayer().getGameMode() == GameMode.SURVIVAL) {
            if(!boostingCooldownedPlayers.contains(e.getPlayer()) && e.getPlayer().getLocation().getY() < e.getPlayer().getWorld().getSpawnLocation().getY() + 40) {
                boostingCooldownedPlayers.add(e.getPlayer());

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        boostingCooldownedPlayers.remove(e.getPlayer());
                    }
                }.runTaskLater(NoxetServer.getPlugin(), 40);

                e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 0.5f);
                e.getPlayer().setVelocity(new Vector(0, 1, 0).add(e.getPlayer().getLocation().getDirection().multiply(2)));
            }

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
    public void onPlayerInteract(PlayerInteractEvent e) {
        if(e.getItem() != null && (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            if(e.getItem().equals(HubInventory.getGameNavigator())) {
                new GameNavigationMenu().openInventory(e.getPlayer());
            } else
                return;

            e.setCancelled(true);
        } else if(NoxetServer.isWorldPreserved(e.getPlayer().getWorld()) && !(e.getAction() == Action.RIGHT_CLICK_BLOCK && e.getClickedBlock() != null && e.getClickedBlock().getType() == Material.ENDER_CHEST)) {
            e.setCancelled(true);
        } else if(e.getAction() == Action.LEFT_CLICK_AIR && ChickenLeg.isPlayerChickenLeg(e.getPlayer()))
            ChickenLeg.summonChickenLeg(e.getPlayer());

    }

    private static final Set<Player> invulnerablePlayers = new HashSet<>();
    
    public static void setTemporaryInvulnerability(Player player) {
        if(invulnerablePlayers.contains(player))
            return;

        invulnerablePlayers.add(player);

        int ticksInvulnerable = 120;

        for(int i = ticksInvulnerable; i > 0; i -= 20) {
            int finalI = i;
            new BukkitRunnable() {
                @Override
                public void run() {
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§e§lINVULNERABLE §c" + (finalI / 20) + "s"));
                }
            }.runTaskLater(NoxetServer.getPlugin(), ticksInvulnerable - i);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§cYou are no longer invulnerable."));
                invulnerablePlayers.remove(player);
            }
        }.runTaskLater(NoxetServer.getPlugin(), 120);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent e) {
        if(e.getEntity() instanceof Player) {
            if(NoxetServer.isWorldSafeZone(e.getEntity().getWorld())) {
                if(e.getCause() == EntityDamageEvent.DamageCause.VOID && e.getEntity().getLocation().getY() < e.getEntity().getWorld().getMinHeight())
                    goToSpawn((Player) e.getEntity());
                e.setCancelled(true);
            } else if(invulnerablePlayers.contains((Player) e.getEntity())) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if(e.getWhoClicked().getWorld() == NoxetServer.ServerWorld.HUB.getWorld() || invulnerablePlayers.contains((Player) e.getWhoClicked()))
            e.setCancelled(true);
    }

    @EventHandler
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent e) {
        if(invulnerablePlayers.contains(e.getPlayer()))
            e.setCancelled(true);
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
        handlePortalTeleport(e.getFrom(), e.getTo());
    }

    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent e) {
        handlePortalTeleport(e.getFrom(), e.getTo());
    }

    private void handlePortalTeleport(Location from, Location to) {
        World sourceWorld = from.getWorld();
        Realm realm = getRealmFromWorld(sourceWorld);

        if(realm == null)
            return; // No realm, we don't have to handle this.

        if(to == null)
            return;

        World realmOverworld = realm.getWorld(NoxetServer.WorldFlag.OVERWORLD),
                realmNether = realm.getWorld(NoxetServer.WorldFlag.NETHER),
                realmEnd = realm.getWorld(NoxetServer.WorldFlag.END);

        World originWorld = from.getWorld();

        switch(Objects.requireNonNull(to.getWorld()).getEnvironment()) {
            case NETHER: // Overworld <-> Nether (to and from)
                if(originWorld == realmOverworld) // Overworld to Nether
                    to.setWorld(realmNether);
                else // Nether to Overworld
                    to.setWorld(realmOverworld);

                break;
            case THE_END: // Overworld <-> End (to and from)
                if(originWorld == realmOverworld) // Overworld to End
                    to.setWorld(realmEnd);
                else // End to Overworld (end credits)
                    to.setWorld(realmOverworld);

                break;
        }
    }

    private static final Map<Player, Location> unconfirmedPlayerRespawns = new HashMap<>();

    public static void abortUnconfirmedPlayerRespawn(Player player) {
        if(unconfirmedPlayerRespawns.remove(player) != null)
            new NoxetMessage("§cYour respawn location was not changed.").send(player);
    }

    @EventHandler
    public void onPlayerSpawnChange(PlayerSpawnChangeEvent e) {
        Location oldBedSpawn = e.getPlayer().getBedSpawnLocation();

        if(oldBedSpawn != null && e.getNewSpawn() != null && oldBedSpawn.getBlock().getLocation().distance(e.getNewSpawn()) > 2 && e.getCause() == PlayerSpawnChangeEvent.Cause.BED) {
            unconfirmedPlayerRespawns.put(e.getPlayer(), e.getNewSpawn());

            new BukkitRunnable() {
                @Override
                public void run() {
                    e.getPlayer().spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§cYour spawn location was §lNOT§c changed! Read chat."));
                }
            }.runTaskLater(NoxetServer.getPlugin(), 0);

            new NoxetMessage("§e§lIMPORTANT!§c You already have a respawn location. Are you sure that you want to replace it?")
                    .addButton("Replace", ChatColor.YELLOW, "Set this as your new spawn", TemporaryCommand.CONFIRM_BED_SPAWN.getRawCommand())
                    .send(e.getPlayer());

            new BukkitRunnable() {
                @Override
                public void run() {
                    if(unconfirmedPlayerRespawns.remove(e.getPlayer()) != null)
                        new NoxetMessage("§c§lWARNING! §cYour spawn was NOT changed.");
                }
            }.runTaskLater(NoxetServer.getPlugin(), 20 * 60);

            e.setCancelled(true);
        }
    }
}
