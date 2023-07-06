package org.noxet.noxetserver.minigames.worldeater;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.noxet.noxetserver.NoxetServer;
import org.noxet.noxetserver.messaging.Message;
import org.noxet.noxetserver.minigames.MiniGameController;
import org.noxet.noxetserver.util.Promise;

import java.util.Random;
import java.util.function.BiConsumer;

public class WorldEaterEvents {
    public enum GameEvent {
        STALKER_CHICKEN("Stalker chicken", WorldEaterEvents::stalkerChicken),
        METEOR_RAIN("Meteor rain", WorldEaterEvents::meteorRain),
        VISIBLE_HIDERS("Hiders are exposed", WorldEaterEvents::visibleHiders),
        DRILLING("Drilling", WorldEaterEvents::drilling),
        EXPLODING_HORSES("Exploding horses are incoming", WorldEaterEvents::explodingHorses),
        EVERYONE_VISIBLE("Everyone are visible", WorldEaterEvents::everyoneVisible);

        private final String eventName;

        private final BiConsumer<WorldEater, Promise> eventConsumer;

        GameEvent(String eventName, BiConsumer<WorldEater, Promise> eventConsumer) {
            this.eventName = eventName;
            this.eventConsumer = eventConsumer;
        }

        public String getEventName() {
            return eventName;
        }

        public BiConsumer<WorldEater, Promise> getEventConsumer() {
            return eventConsumer;
        }
    }

    public static void stalkerChicken(WorldEater worldEater, Promise promise) {
        worldEater.playGameSound(Sound.ENTITY_CHICKEN_AMBIENT, 1, 2);
        worldEater.sendGameMessage(new Message("§e§lSTALKER CHICKEN! §fA chicken has spawned. It is glowing, so you can see it from anywhere.\nThe chicken will follow its nearest player.\nThe chicken cannot be eliminated.\nIt will disappear in 1 minute."));

        Mob chicken = (Mob) MiniGameController.getMiniGameWorld().spawnEntity(worldEater.getSpawnLocation(), EntityType.ZOMBIE);

        chicken.setGlowing(true);
        chicken.setInvulnerable(true);

        chicken.setLootTable(null);

        worldEater.addTask(new BukkitRunnable() {
            @Override
            public void run() {
                if(chicken.getTicksLived() > 20 * 60) {
                    chicken.remove();
                    cancel();
                    promise.report();
                    worldEater.sendGameMessage(new Message("§eThe chicken left the game."));
                    return;
                }

                Player nearestPlayer = null;

                for(Player player : worldEater.getPlayers())
                    if(nearestPlayer == null || player.getLocation().distanceSquared(chicken.getLocation()) < nearestPlayer.getLocation().distanceSquared(chicken.getLocation()))
                        nearestPlayer = player;

                if(nearestPlayer == null)
                    return;

                if(nearestPlayer != chicken.getTarget()) {
                    nearestPlayer.playSound(nearestPlayer, Sound.ITEM_ARMOR_EQUIP_NETHERITE, 1, 0.5f);
                    chicken.setTarget(nearestPlayer);
                    worldEater.sendGameMessage(new Message("§cThe chicken is now stalking " + nearestPlayer.getName() + "."));
                }

                MiniGameController.getMiniGameWorld().spawnParticle(Particle.NOTE, chicken.getLocation().add(0, 1, 0), 5);
            }
        }.runTaskTimer(NoxetServer.getPlugin(), 60, 30));
    }

    public static void meteorRain(WorldEater worldEater, Promise promise) {
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

                    Fireball meteor = MiniGameController.getMiniGameWorld().spawn(meteorStart, Fireball.class);

                    meteor.setIsIncendiary(true);
                    meteor.setYield(8);

                    meteor.setDirection(targetLocation.toVector().subtract(meteorStart.toVector()));

                    if(isLast)
                        promise.report();
                }
            }.runTaskLater(NoxetServer.getPlugin(), 20 * (i + 1) * 15));
        }
    }

    public static void visibleHiders(WorldEater worldEater, Promise promise) {
        worldEater.playGameSound(Sound.ITEM_GOAT_HORN_SOUND_7, 1, 0.8f);
        worldEater.sendGameMessage(new Message("§c§lALERT! §eHiders are now visible for 10 seconds!"));

        worldEater.getTeamSet().forEach(WorldEaterTeams.HIDER, hider -> {
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
                promise.report();
            }
        }.runTaskLater(NoxetServer.getPlugin(), 20 * 10));
    }

    public static void drilling(WorldEater worldEater, Promise promise) {
        worldEater.playGameSound(Sound.ITEM_GOAT_HORN_SOUND_4, 1, 2f);
        worldEater.sendGameMessage(new Message("§c§lDRILLING! §cDrills will now be performed randomly. A whole Y-axis will be drilled down into void!"));
        Random random = new Random();

        final int drillHoles = 15;
        for(int i = 0; i < drillHoles; i++) {
            boolean isLast = i == drillHoles - 1;

            worldEater.addTask(new BukkitRunnable() {
                @Override
                public void run() {
                    Location drillLocation = worldEater.getCenterChunk().getBlock(
                            random.nextInt(0, 16),
                            0,
                            random.nextInt(0, 16)
                    ).getLocation();

                    int yMax = MiniGameController.getMiniGameWorld().getMaxHeight(), yMin = MiniGameController.getMiniGameWorld().getMinHeight();

                    for(int y = yMin; y < yMax; y++) {
                        Location drillBlock = drillLocation.clone();
                        drillBlock.setY(y);

                        MiniGameController.getMiniGameWorld().spawnParticle(Particle.SWEEP_ATTACK, drillBlock, 3);
                        int finalY = y;
                        boolean isLast2 = isLast && y == yMin + 1;

                        worldEater.addTask(new BukkitRunnable() {
                            @Override
                            public void run() {
                                if(finalY % 2 == 0)
                                    MiniGameController.getMiniGameWorld().playSound(drillBlock, Sound.BLOCK_BAMBOO_BREAK, 1, 2f);

                                MiniGameController.getMiniGameWorld().spawnParticle(Particle.SWEEP_ATTACK, drillBlock, 5);
                                drillBlock.getBlock().setBlockData(Material.AIR.createBlockData(), false);

                                if(isLast2)
                                    promise.report();
                            }
                        }.runTaskLater(NoxetServer.getPlugin(), 2L * (yMax - y)));
                    }
                }
            }.runTaskLater(NoxetServer.getPlugin(), 20 * 15 * (i + 1)));
        }
    }

    public static void explodingHorses(WorldEater worldEater, Promise promise) {
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

                        Horse horse = (Horse) MiniGameController.getMiniGameWorld().spawnEntity(unluckyPlayer.getLocation(), EntityType.HORSE);

                        horse.setVisualFire(true);
                        horse.setHealth(0.5);

                        worldEater.addTask(new BukkitRunnable() {
                            @Override
                            public void run() {
                                if(!horse.isDead()) {
                                    MiniGameController.getMiniGameWorld().playSound(horse.getLocation(), Sound.ENTITY_GHAST_SCREAM, 1, 0.5f);
                                    horse.remove();
                                    MiniGameController.getMiniGameWorld().createExplosion(horse.getLocation(), 12);
                                }

                                if(isLast)
                                    promise.report();
                            }
                        }.runTaskLater(NoxetServer.getPlugin(), 20 * 4));
                    }
                }
            }.runTaskLater(NoxetServer.getPlugin(), 20 * 3 * i));
        }
    }

    public static void everyoneVisible(WorldEater worldEater, Promise promise) {
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

        worldEater.addTask(new BukkitRunnable() {
            @Override
            public void run() {
                promise.report();
            }
        }.runTaskLater(NoxetServer.getPlugin(), 20 * 60));
    }
}
