package org.noxet.noxetserver.minigames.worldeater;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.noxet.noxetserver.NoxetServer;
import org.noxet.noxetserver.messaging.Message;

import java.util.Random;

public class WorldEaterEvents {
    public enum GameEvent {
        METEOR_RAIN("Meteor rain"),
        VISIBLE_HIDERS("Hiders are exposed"),
        DRILLING("Drilling"),
        SHRINKING_WORLD_BORDER("World border is shrinking"),
        EXPLODING_HORSES("Exploding horses are incoming"),
        EVERYONE_VISIBLE("Everyone are visible");

        public final String eventName;

        GameEvent(String eventName) {
            this.eventName = eventName;
        }
    }

    public static void meteorRain(WorldEater worldEater) {
        worldEater.playGameSound(Sound.ITEM_GOAT_HORN_SOUND_3, 1, 2);
        worldEater.sendGameMessage(new Message("§c§lMETEOR RAIN! §cHead to shelter!"));

        final int meteorAmount = 10;
        for(int i = 0; i < meteorAmount; i++) {
            boolean isLast = i == meteorAmount - 1;
            worldEater.addTask(new BukkitRunnable() {
                @Override
                public void run() {
                    Player meteorTarget = worldEater.getRandomPlayer(true);

                    meteorTarget.playSound(meteorTarget, Sound.ITEM_GOAT_HORN_SOUND_0, 1, 0.5f);

                    Location targetLocation = meteorTarget.getLocation();

                    Random random = new Random();

                    Location meteorStart = targetLocation.clone();
                    meteorStart.add(random.nextInt(-50, 50), random.nextInt(50, 100), random.nextInt(-50, 50));

                    Fireball meteor = worldEater.getWorkingWorld().spawn(meteorStart, Fireball.class);

                    meteor.setIsIncendiary(true);
                    meteor.setYield(8);

                    meteor.setDirection(targetLocation.toVector().subtract(meteorStart.toVector()));

                    if(isLast)
                        worldEater.removeEvent(GameEvent.METEOR_RAIN);
                }
            }.runTaskLater(NoxetServer.getPlugin(), 20 * (i + 1) * 15));
        }
    }

    public static void visibleHiders(WorldEater worldEater) {
        worldEater.playGameSound(Sound.ITEM_GOAT_HORN_SOUND_7, 1, 0.8f);
        worldEater.sendGameMessage(new Message("§c§lALERT! §eHiders are now visible for 10 seconds!"));

        worldEater.forEachHider(hider -> {
            hider.sendTitle("§c§lEXPOSED!", "§eYour location is now visible.", 5, 20 * 10, 5);
            hider.addPotionEffect(
                    new PotionEffect(
                            PotionEffectType.GLOWING, 20 * 10, 10, true, false, false
                    )
            );
        });

        worldEater.addTask(new BukkitRunnable() {
            @Override
            public void run() {
                worldEater.removeEvent(WorldEaterEvents.GameEvent.VISIBLE_HIDERS);
            }
        }.runTaskLater(NoxetServer.getPlugin(), 20 * 10));
    }

    public static void drilling(WorldEater worldEater) {
        worldEater.playGameSound(Sound.ITEM_GOAT_HORN_SOUND_4, 1, 2f);
        worldEater.sendGameMessage(new Message("§c§lDRILLING! §cDrills will now be performed randomly. A whole Y-axis will be drilled down into void!"));
        Random random = new Random();

        final int drillHoles = 15;
        for(int i = 0; i < drillHoles; i++) {
            boolean isLast = i == drillHoles - 1;

            worldEater.addTask(new BukkitRunnable() {
                @Override
                public void run() {
                    Location drillLocation = new Location(worldEater.getWorkingWorld(), random.nextInt(0, 16), 0, random.nextInt(0, 16));
                    int yMax = worldEater.getWorkingWorld().getMaxHeight(), yMin = worldEater.getWorkingWorld().getMinHeight();

                    for(int y = yMin; y < yMax; y++) {
                        Location drillBlock = drillLocation.clone();
                        drillBlock.setY(y);

                        worldEater.getWorkingWorld().spawnParticle(Particle.SWEEP_ATTACK, drillBlock, 3);
                        int finalY = y;
                        boolean isLast2 = isLast && y == yMin + 1;

                        worldEater.addTask(new BukkitRunnable() {
                            @Override
                            public void run() {
                                if(finalY % 2 == 0)
                                    worldEater.getWorkingWorld().playSound(drillBlock, Sound.BLOCK_BAMBOO_BREAK, 1, 2f);

                                worldEater.getWorkingWorld().spawnParticle(Particle.SWEEP_ATTACK, drillBlock, 5);
                                drillBlock.getBlock().setBlockData(Material.AIR.createBlockData(), false);

                                if(isLast2)
                                    worldEater.removeEvent(WorldEaterEvents.GameEvent.DRILLING);
                            }
                        }.runTaskLater(NoxetServer.getPlugin(), 2L * (yMax - y)));
                    }
                }
            }.runTaskLater(NoxetServer.getPlugin(), 20 * 15 * (i + 1)));
        }
    }

    public static void shrinkingWorldBorder(WorldEater worldEater) {
        worldEater.playGameSound(Sound.ITEM_GOAT_HORN_SOUND_6, 1, 2);
        worldEater.sendGameMessage(new Message("§eThe world border will shrink in §c30§e seconds!"));

        worldEater.addTask(new BukkitRunnable() {
            @Override
            public void run() {
                worldEater.playGameSound(Sound.ITEM_GOAT_HORN_SOUND_6, 1, 0.5f);

                worldEater.getWorkingWorld().getWorldBorder().setSize(32);
                worldEater.getWorkingWorld().getWorldBorder().setWarningTime(20);
                worldEater.getWorkingWorld().getWorldBorder().setCenter(worldEater.getSpawnLocation());
                worldEater.sendGameMessage(new Message("§eWorld border has shrunk!"));

                worldEater.removeEvent(WorldEaterEvents.GameEvent.SHRINKING_WORLD_BORDER);
            }
        }.runTaskLater(NoxetServer.getPlugin(), 20 * 30));
    }

    public static void explodingHorses(WorldEater worldEater) {
        worldEater.sendGameMessage(new Message("§8<§k-§8> §4§lSUDDEN DEATH! §cExploding horses will appear. They may be killed with a single hit, but - if not - they will put you down."));

        worldEater.playGameSound(Sound.ENTITY_HORSE_ANGRY, 5, 5);

        final int horseCount = 20;

        for(int i = 0; i < horseCount; i++) {
            boolean isLast = i == horseCount - 1;

            worldEater.addTask(new BukkitRunnable() {
                @Override
                public void run() {
                    if(Math.random() * 10 < 3) {
                        Player unluckyPlayer = worldEater.getRandomPlayer();
                        unluckyPlayer.playSound(unluckyPlayer, Sound.ENTITY_HORSE_ANGRY, 6, 6);

                        Horse horse = (Horse) worldEater.getWorkingWorld().spawnEntity(unluckyPlayer.getLocation(), EntityType.HORSE);

                        horse.setVisualFire(true);
                        horse.setHealth(0.5);

                        worldEater.addTask(new BukkitRunnable() {
                            @Override
                            public void run() {
                                if(!horse.isDead()) {
                                    worldEater.getWorkingWorld().playSound(horse.getLocation(), Sound.ENTITY_GHAST_SCREAM, 1, 0.5f);
                                    horse.remove();
                                    worldEater.getWorkingWorld().createExplosion(horse.getLocation(), 12);
                                }

                                if(isLast)
                                    worldEater.removeEvent(WorldEaterEvents.GameEvent.EXPLODING_HORSES);
                            }
                        }.runTaskLater(NoxetServer.getPlugin(), 20 * 4));
                    }
                }
            }.runTaskLater(NoxetServer.getPlugin(), 20 * 3 * i));
        }
    }

    public static void everyoneVisible(WorldEater worldEater) {
        worldEater.playGameSound(Sound.ITEM_GOAT_HORN_SOUND_7, 1, 0.5f);
        worldEater.sendGameMessage(new Message("§c§lALERT! §eEVERYONE are now visible!"));

        worldEater.forEachPlayer(player -> {
            player.sendTitle("§c§lEXPOSED!", "§eEveryone can now see everyone.", 5, 20 * 5, 5);
            player.addPotionEffect(
                    new PotionEffect(
                            PotionEffectType.GLOWING, 20 * 60, 10, true, false, false
                    )
            );
        });
    }
}
