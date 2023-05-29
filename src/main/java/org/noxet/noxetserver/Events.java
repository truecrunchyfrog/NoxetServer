package org.noxet.noxetserver;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
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
import org.noxet.noxetserver.amateurgenerators.NetherPortalGenerator;
import org.noxet.noxetserver.commands.teleportation.TeleportAsk;
import org.noxet.noxetserver.inventory.HubInventory;
import org.noxet.noxetserver.inventory.menus.GameNavigationMenu;
import org.noxet.noxetserver.messaging.Motd;
import org.noxet.noxetserver.messaging.NoxetErrorMessage;
import org.noxet.noxetserver.messaging.NoxetMessage;

import java.util.*;

import static org.noxet.noxetserver.RealmManager.*;

public class Events implements Listener {
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
        new NoxetMessage("§3§lWELCOME TO §b§lNOXET.ORG§3§l!").send(e.getPlayer());
        new NoxetMessage("§f" + e.getPlayer().getDisplayName() + "§7 joined Noxet.org.").broadcast();
        e.setJoinMessage(null);

        if(!((boolean) new PlayerDataManager(e.getPlayer()).get(PlayerDataManager.Attribute.HAS_DONE_CAPTCHA))) {
            new Captcha(e.getPlayer()).init();
            return;
        }

        Bukkit.getScheduler().scheduleSyncDelayedTask(NoxetServer.getPlugin(), () -> {
            Realm realm = getCurrentRealm(e.getPlayer());

            if(realm != null) {
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
                        player.playSound(player.getLocation(), Sound.ENTITY_CHICKEN_DEATH, 1, 2 - 1.5f * ((float) finalI / totalPlays));
                        player.playSound(player.getLocation(), Sound.BLOCK_BELL_USE, 1 * ((float) finalI / totalPlays), 2 - 1.5f * ((float) finalI / totalPlays));
                    }
                }.runTaskLater(NoxetServer.getPlugin(), i * 10);
            }
        }, 2);

        new NoxetMessage("§c" + e.getDeathMessage() + ".").send(getCurrentRealm(player));
        e.setDeathMessage(null);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent e) {
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

    @EventHandler
    public void onAsyncPlayerChat(AsyncPlayerChatEvent e) {
        if(Captcha.isPlayerDoingCaptcha(e.getPlayer()))
            e.setCancelled(true);
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
        } else if(e.getMessage().equals("/bedspawnconfirm")) {
            e.setCancelled(true);

            if(!unconfirmedPlayerRespawns.containsKey(e.getPlayer())) {
                new NoxetErrorMessage("You cannot do this now.").send(e.getPlayer());
                return;
            }

            Location newBedSpawn = unconfirmedPlayerRespawns.remove(e.getPlayer());
            e.getPlayer().setBedSpawnLocation(newBedSpawn);

            if(e.getPlayer().getBedSpawnLocation() != null && newBedSpawn.getBlock().getType().name().endsWith("_BED"))
                new NoxetMessage("§aGreat! Your respawn location has been updated.").send(e.getPlayer());
            else
                new NoxetErrorMessage("Could not change your respawn location.").send(e.getPlayer());
        }
    }

    private static final List<Player> boostingCooldownedPlayers = new ArrayList<>();

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
                e.getPlayer().openInventory(new GameNavigationMenu().getInventory());
            } else {
                return;
            }

            e.setCancelled(true);
        } else if(NoxetServer.isWorldPreserved(e.getPlayer().getWorld()) && !(e.getAction() == Action.RIGHT_CLICK_BLOCK && e.getClickedBlock() != null && e.getClickedBlock().getType() == Material.ENDER_CHEST))  {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent e) {
        if(e.getEntity() instanceof Player && NoxetServer.isWorldSafeZone(e.getEntity().getWorld())) {
            if(e.getCause() == EntityDamageEvent.DamageCause.VOID && e.getEntity().getLocation().getY() < e.getEntity().getWorld().getMinHeight())
                goToSpawn((Player) e.getEntity());
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if(e.getWhoClicked().getWorld() == NoxetServer.ServerWorld.HUB.getWorld())
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
                    player.openInventory(new GameNavigationMenu().getInventory());
                }
            }.runTaskLater(NoxetServer.getPlugin(), 5);
        }
    }

    @EventHandler
    public void onEntityPortal(EntityPortalEvent e) {
    }

    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent e) {
        World sourceWorld = e.getFrom().getWorld();
        Realm realm = getRealmFromWorld(sourceWorld);

        if(realm == null)
            return; // No realm, we don't have to handle this.

        switch(e.getCause()) {
            case NETHER_PORTAL: // Overworld <-> Nether (to and from)
                Location searchStart = e.getFrom();

                if(e.getPlayer().getWorld().equals(realm.getWorld(NoxetServer.WorldFlag.OVERWORLD))) {
                    // Overworld to Nether
                    searchStart.setWorld(realm.getWorld(NoxetServer.WorldFlag.NETHER));
                    searchStart.multiply(1d / 8);
                } else {
                    // Nether to Overworld
                    searchStart.setWorld(realm.getWorld(NoxetServer.WorldFlag.OVERWORLD));
                    searchStart.multiply(8);
                }

                e.setTo(searchStart);
                //NetherPortalGenerator.generatePortal(searchStart);
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
        if(e.getPlayer().getBedSpawnLocation() != null && e.getCause() == PlayerSpawnChangeEvent.Cause.BED) {
            unconfirmedPlayerRespawns.put(e.getPlayer(), e.getNewSpawn());

            new BukkitRunnable() {
                @Override
                public void run() {
                    e.getPlayer().spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§cYour spawn location was §lNOT§c changed! Read chat."));
                }
            }.runTaskLater(NoxetServer.getPlugin(), 0);

            new NoxetMessage("§e§l§nIMPORTANT! §cYou already have a respawn location. Are you sure that you want to replace it?")
                    .addButton("Replace", ChatColor.YELLOW, "Set this as your new spawn", "bedspawnconfirm")
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
