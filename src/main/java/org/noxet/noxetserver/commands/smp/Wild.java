package org.noxet.noxetserver.commands.smp;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Biome;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.noxet.noxetserver.NoxetServer;
import org.noxet.noxetserver.RealmManager;
import org.noxet.noxetserver.messaging.NoxetErrorMessage;
import org.noxet.noxetserver.messaging.NoxetMessage;
import org.noxet.noxetserver.util.TeleportUtil;

import java.text.DecimalFormat;
import java.util.*;

import static org.noxet.noxetserver.RealmManager.getCurrentRealm;

@SuppressWarnings("ALL")
public class Wild implements CommandExecutor {
    private static final Set<Player> recentlyTeleported = new HashSet<>();
    private static final List<Biome> wildBiomesExceptions = Arrays.asList(
            Biome.OCEAN,
            Biome.FROZEN_OCEAN,
            Biome.COLD_OCEAN,
            Biome.LUKEWARM_OCEAN,
            Biome.WARM_OCEAN,
            Biome.DEEP_OCEAN,
            Biome.DEEP_COLD_OCEAN,
            Biome.DEEP_LUKEWARM_OCEAN,
            Biome.DEEP_FROZEN_OCEAN,
            Biome.RIVER,
            Biome.FROZEN_RIVER
    );

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(!(commandSender instanceof Player)) {
            new NoxetErrorMessage(NoxetErrorMessage.ErrorType.COMMON, "Only players can go to the wild.").send(commandSender);
            return true;
        }

        Player player = (Player) commandSender;

        if(recentlyTeleported.contains(player)) {
            new NoxetErrorMessage(NoxetErrorMessage.ErrorType.COMMON, "You recently got a wilderness teleport. You must wait 2 minutes between requests.").send(player);
            return true;
        }

        RealmManager.Realm realm = getCurrentRealm(player);

        if(realm == null || !realm.doesAllowTeleportationMethods()) {
            new NoxetErrorMessage(NoxetErrorMessage.ErrorType.COMMON, "Wilderness teleportation is not supported here.").send(player);
            return true;
        }

        new NoxetMessage("Wilderness teleportation commencing. Please wait ...").send(player);

        Bukkit.getScheduler().scheduleSyncDelayedTask(NoxetServer.getPlugin(), () -> {
            Location teleportTo = new Location(
                    realm.getWorld(NoxetServer.WorldFlag.OVERWORLD),
                    0, 0, 0
            );

            int attemptsLeft = 300;

            do {
                if(attemptsLeft-- == 0) {
                    new NoxetErrorMessage(NoxetErrorMessage.ErrorType.COMMON, "Uh oh. We searched far and wide, but we could not find a suiting place for you.").send(player);
                    break;
                }

                teleportTo.setX(getWildXZValue());
                teleportTo.setZ(getWildXZValue());

                teleportTo.setY(Objects.requireNonNull(teleportTo.getWorld()).getHighestBlockYAt(teleportTo));
            } while(
                    wildBiomesExceptions.contains(teleportTo.getWorld().getBiome(teleportTo)) || // Look for new location until it is not listed in the blacklist.
                    !TeleportUtil.isLocationTeleportSafe(teleportTo)
            );

            player.teleport(teleportTo);

            new BukkitRunnable() {
                @Override
                public void run() {
                    if(!teleportTo.getChunk().isLoaded())
                        return;

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            new NoxetMessage("Welcome to the wild!").send(player);

                            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 420, 10, false, false));
                            player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 420, 100, false, false));

                            player.playSound(player.getLocation(), Sound.ITEM_GOAT_HORN_SOUND_2, 1, 0.5f);

                            player.sendTitle("§2§lINTO THE WILDERNESS!", "§f(§a§l" + ((int) player.getLocation().getX()) + "§7; §a§l" + ((int) player.getLocation().getZ()) + "§f)", 0, 200, 0);

                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    player.playSound(player.getLocation(), Sound.ITEM_GOAT_HORN_SOUND_3, 1, 0.5f);
                                    player.sendTitle("§e§l" + new DecimalFormat("#,###").format((int) player.getLocation().distance(new Location(player.getLocation().getWorld(), 0, player.getLocation().getY(), 0))), "§6blocks away from (0; 0).", 0, 200, 0);
                                }
                            }.runTaskLater(NoxetServer.getPlugin(), 200);
                        }
                    }.runTaskLater(NoxetServer.getPlugin(), 60);

                    this.cancel(); // Stop this timer.
                }
            }.runTaskTimer(NoxetServer.getPlugin(), 20, 20);
        }, 0);

        recentlyTeleported.add(player);

        new BukkitRunnable() {
            @Override
            public void run() {
                recentlyTeleported.remove(player);
            }
        }.runTaskLater(NoxetServer.getPlugin(), 20 * 60 * 2);

        return true;
    }

    private static final int wildBound = 15 * (int) Math.pow(10, 3);

    int getWildXZValue() {
        Random random = new Random();
        return random.nextInt(-wildBound, wildBound);
    }
}
