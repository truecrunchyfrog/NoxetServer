package org.noxet.noxetserver.minigames.worldeater;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.noxet.noxetserver.messaging.Message;
import org.noxet.noxetserver.minigames.MiniGameController;
import org.noxet.noxetserver.util.EntityGlowColor;
import org.noxet.noxetserver.util.Promise;

import java.util.Random;
import java.util.function.BiConsumer;

public class WorldEaterEvents {
    public enum GameEvent {
        HOT_SUN("Hot sun", WorldEaterEvents::hotSun),
        QUICK_STOVE("Quick stove", WorldEaterEvents::quickStove),
        METEOR_RAIN("Meteor rain", WorldEaterEvents::meteorRain),
        VISIBLE_HIDERS("Exposed hiders", WorldEaterEvents::visibleHiders),
        LOOT_DROP("Loot drop", WorldEaterEvents::lootDrop),
        DRILLING("Drilling", WorldEaterEvents::drilling),
        EXPLODING_HORSES("Exploding horses", WorldEaterEvents::explodingHorses),
        EVERYONE_VISIBLE("Everyone exposed", WorldEaterEvents::everyoneVisible);

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

    public static void hotSun(WorldEater worldEater, Promise promise) {
        worldEater.playGameSound(Sound.ENTITY_PLAYER_HURT_ON_FIRE, 1, 2);
        worldEater.sendGameMessage(new Message("§6§lHOT SUN! §6The sun is on fire! Get in shelter, or you will too..."));

        worldEater.scheduleTaskTimer(task -> {
            if(promise.isReported())
                task.cancel();

            for(Player player : worldEater.getPlayers())
                if(MiniGameController.getMiniGameWorld().getHighestBlockYAt(player.getLocation()) < player.getLocation().getY())
                    player.setFireTicks(30);
        }, 20 * 5, 20);

        worldEater.scheduleTask(promise::report, 20 * 60 * 2);
    }

    public static void quickStove(WorldEater worldEater, Promise promise) {
        worldEater.playGameSound(Sound.ITEM_GOAT_HORN_PLAY, 1, 2);
        worldEater.sendGameMessage(new Message("§e§lQUICK STOVE! §eFurnaces will now not only DUPE what's cooked, it will also cook at FIVE TIMES (5x) the speed! Get on your grill now, because this will only last for 2 minutes."));

        worldEater.scheduleTask(promise::report, 20 * 60 * 2);
    }

    public static void meteorRain(WorldEater worldEater, Promise promise) {
        worldEater.playGameSound(Sound.ITEM_GOAT_HORN_SOUND_3, 1, 2);
        worldEater.sendGameMessage(new Message("§c§lMETEOR RAIN! §cHead to shelter!\nWhen you hear a horn, a meteor is flying towards you. "));

        Random random = new Random();

        final int meteorAmount = 10;
        for(int i = 0; i < meteorAmount; i++) {
            boolean isLast = i == meteorAmount - 1;
            worldEater.scheduleTask(() -> {
                    Player meteorTarget = worldEater.getRandomPlayer(true);

                    meteorTarget.playSound(meteorTarget, Sound.ITEM_GOAT_HORN_SOUND_0, 1, 0.5f);
                    worldEater.sendGameMessage(new Message("§5§l§k### ### ### ### §c" + meteorTarget.getName() + "§4 is targeted by a meteor."));

                    Location targetLocation = meteorTarget.getLocation();

                    Location meteorStart = targetLocation.clone();
                    meteorStart.add(random.nextInt(-50, 50), random.nextInt(30, 100), random.nextInt(-50, 50));

                    Fireball meteor = MiniGameController.getMiniGameWorld().spawn(meteorStart, Fireball.class);

                    EntityGlowColor.setGlowColor(meteor, ChatColor.RED);
                    meteor.setGlowing(true);
                    meteor.setIsIncendiary(true);
                    meteor.setYield(8);

                    meteor.setDirection(targetLocation.toVector().subtract(meteorStart.toVector()));

                    if(isLast)
                        promise.report();
                }, 20 * (i + 1) * 15);
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

        worldEater.scheduleTask(promise::report, 20 * 10);
    }

    public static void lootDrop(WorldEater worldEater, Promise promise) {
        worldEater.playGameSound(Sound.ENTITY_CAMEL_SADDLE, 1, 0.5f);
        worldEater.sendGameMessage(new Message("§9§lLOOT DROP! §9Look up for a falling loot box, with only the best to offer."));
        Random random = new Random();

        Location dropLocation = worldEater.getCenterChunk().getBlock(random.nextInt(16), 0, random.nextInt(16)).getLocation();

        dropLocation.setY(MiniGameController.getMiniGameWorld().getHighestBlockYAt(dropLocation) + 100);

        FallingBlock fallingLootBox = MiniGameController.getMiniGameWorld().spawnFallingBlock(dropLocation, Material.BARREL.createBlockData());

        EntityGlowColor.setGlowColor(fallingLootBox, ChatColor.BLUE);
        fallingLootBox.setGlowing(true);
        fallingLootBox.setDropItem(false);

        worldEater.scheduleTaskTimer(task -> {
            if(promise.isReported() || fallingLootBox.isDead()) {
                promise.report();
                task.cancel();
            }
        }, 120, 20);

        worldEater.scheduleTask(promise::report, 20 * 100);
    }

    public static void drilling(WorldEater worldEater, Promise promise) {
        worldEater.playGameSound(Sound.ITEM_GOAT_HORN_SOUND_4, 1, 2f);
        worldEater.sendGameMessage(new Message("§c§lDRILLING! §cDrills will now be performed randomly. A whole Y-axis will be drilled down into void!"));
        Random random = new Random();

        final int drillHoles = 15;
        for(int i = 0; i < drillHoles; i++) {
            boolean isLast = i == drillHoles - 1;

            worldEater.scheduleTask(() -> {
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

                        worldEater.scheduleTask(() -> {
                                if(finalY % 2 == 0)
                                    MiniGameController.getMiniGameWorld().playSound(drillBlock, Sound.BLOCK_BAMBOO_BREAK, 1, 2f);

                                MiniGameController.getMiniGameWorld().spawnParticle(Particle.SWEEP_ATTACK, drillBlock, 5);
                                drillBlock.getBlock().setBlockData(Material.AIR.createBlockData(), false);

                                if(isLast2)
                                    promise.report();
                            }, 2 * (yMax - y));
                    }
                }, 20 * 15 * (i + 1));
        }
    }

    public static void explodingHorses(WorldEater worldEater, Promise promise) {
        worldEater.sendGameMessage(new Message("§8<§k-§8> §4§lSUDDEN DEATH! §cExploding horses will appear. They may be killed with a single hit, but - if not - they will put you down."));

        worldEater.playGameSound(Sound.ENTITY_HORSE_ANGRY, 5, 5);

        final int horseCount = 20;

        for(int i = 0; i < horseCount; i++) {
            boolean isLast = i == horseCount - 1;

            worldEater.scheduleTask(() -> {
                    if(Math.random() * 10 < 3) {
                        Player unluckyPlayer = worldEater.getRandomPlayer();
                        unluckyPlayer.playSound(unluckyPlayer, Sound.ENTITY_HORSE_ANGRY, 6, 6);

                        Horse horse = (Horse) MiniGameController.getMiniGameWorld().spawnEntity(unluckyPlayer.getLocation(), EntityType.HORSE);

                        horse.setVisualFire(true);
                        horse.setHealth(0.5);

                        worldEater.scheduleTask(() -> {
                                if(!horse.isDead()) {
                                    MiniGameController.getMiniGameWorld().playSound(horse.getLocation(), Sound.ENTITY_GHAST_SCREAM, 1, 0.5f);
                                    horse.remove();
                                    MiniGameController.getMiniGameWorld().createExplosion(horse.getLocation(), 12);
                                }

                                if(isLast)
                                    promise.report();
                            }, 20 * 4);
                    }
                }, 20 * 3 * i);
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

        worldEater.scheduleTask(promise::report, 20 * 60);
    }
}
