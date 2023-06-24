package org.noxet.noxetserver;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.noxet.noxetserver.menus.inventory.CaptchaSelectionMenu;
import org.noxet.noxetserver.messaging.ClearChat;
import org.noxet.noxetserver.messaging.Message;
import org.noxet.noxetserver.util.PlayerFreezer;
import org.noxet.noxetserver.util.TextBeautifier;
import org.noxet.noxetserver.playerdata.PlayerDataManager;
import org.noxet.noxetserver.playerstate.PlayerState;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.noxet.noxetserver.RealmManager.goToHub;

public class Captcha {

    public enum CaptchaSound {
        DOG("Wolf", Sound.ENTITY_WOLF_HURT, Material.WOLF_SPAWN_EGG),
        PIG("Pig", Sound.ENTITY_PIG_AMBIENT, Material.PIG_SPAWN_EGG),
        SHEEP("Sheep", Sound.ENTITY_SHEEP_AMBIENT, Material.SHEEP_SPAWN_EGG),
        CREEPER("Creeper", Sound.ENTITY_CREEPER_PRIMED, Material.CREEPER_HEAD),
        GLASS("Glass", Sound.BLOCK_GLASS_BREAK, Material.GLASS),
        EXPLOSION("Explosion", Sound.ENTITY_GENERIC_EXPLODE, Material.TNT),
        PARROT("Parrot", Sound.ENTITY_PARROT_AMBIENT, Material.PARROT_SPAWN_EGG),
        SHULKER_BOX("Shulker Box", Sound.BLOCK_SHULKER_BOX_OPEN, Material.SHULKER_BOX),
        ENDER_DRAGON("Ender Dragon", Sound.ENTITY_ENDER_DRAGON_GROWL, Material.DRAGON_HEAD),
        WITHER("Wither", Sound.ENTITY_WITHER_AMBIENT, Material.WITHER_ROSE),
        CHEST("Chest", Sound.BLOCK_CHEST_OPEN, Material.CHEST),
        ENDERMAN("Enderman", Sound.ENTITY_ENDERMAN_AMBIENT, Material.ENDERMAN_SPAWN_EGG),
        SILVERFISH("Silverfish", Sound.ENTITY_SILVERFISH_AMBIENT, Material.SILVERFISH_SPAWN_EGG),
        DEEPSLATE("Deepslate", Sound.BLOCK_DEEPSLATE_BREAK, Material.DEEPSLATE),
        FIRE("Fire", Sound.BLOCK_FIRE_EXTINGUISH, Material.FLINT_AND_STEEL),
        ANVIL("Anvil", Sound.BLOCK_ANVIL_USE, Material.ANVIL),
        HORSE("Horse", Sound.ENTITY_HORSE_AMBIENT, Material.HORSE_SPAWN_EGG),
        ARMOR("Armor", Sound.ITEM_ARMOR_EQUIP_GENERIC, Material.ARMOR_STAND),
        GRASS("Grass", Sound.BLOCK_GRASS_BREAK, Material.GRASS_BLOCK),
        END_PORTAL("End Portal Frame", Sound.BLOCK_END_PORTAL_FRAME_FILL, Material.END_PORTAL_FRAME),
        NETHER_PORTAL("Nether Portal", Sound.BLOCK_PORTAL_AMBIENT, Material.OBSIDIAN),
        WOOL("Wool", Sound.BLOCK_WOOL_BREAK, Material.WHITE_WOOL),
        DOLPHIN("Dolphin", Sound.ENTITY_DOLPHIN_AMBIENT, Material.DOLPHIN_SPAWN_EGG),
        BURP("Burping", Sound.ENTITY_PLAYER_BURP, Material.COOKED_RABBIT),
        FALL("Fall", Sound.ENTITY_PLAYER_BIG_FALL, Material.PLAYER_HEAD),
        ZOMBIE("Zombie", Sound.ENTITY_ZOMBIE_AMBIENT, Material.ZOMBIE_HEAD),
        SKELETON("Skeleton", Sound.ENTITY_SKELETON_AMBIENT, Material.SKELETON_SKULL),
        VILLAGER("Villager", Sound.ENTITY_VILLAGER_YES, Material.EMERALD);

        private final String name;
        private final Sound sound;
        private final Material material;

        CaptchaSound(String name, Sound sound, Material material) {
            this.name = name;
            this.sound = sound;
            this.material = material;
        }

        public String getName() {
            return name;
        }

        public Sound getSound() {
            return sound;
        }

        public Material getMaterial() {
            return material;
        }
    }

    private static final List<Captcha> captchaInstances = new ArrayList<>();
    private static final PlayerFreezer freezer = new PlayerFreezer(20);

    private final Player player;
    private final Location assignedLocation;
    private int lastQuestion;
    private CaptchaSound correctAnswer = null;
    private final List<BukkitTask> bukkitTasks = new ArrayList<>();
    private BukkitTask timeoutTask;

    public static final int answersPerQuestion = 3, totalQuestions = 5;

    public Captcha(Player player) {
        stopPlayerCaptcha(player); // Stop any already existing captcha.
        captchaInstances.add(this);

        this.player = player;

        Random random = new Random();
        assignedLocation = new Location(getWorld(), random.nextInt(-500, 500), 0, random.nextInt(-500, 500));
    }

    public Player getPlayer() {
        return player;
    }

    public void init() {
        RealmManager.migratingPlayers.add(player); // Prevent migration manager from interacting with player.

        PlayerState.prepareDefault(player);

        getWorld().setBlockData(assignedLocation.clone().subtract(0, 1, 0), Material.BARRIER.createBlockData()); // Place standing block.

        freezer.freeze(player, assignedLocation);

        player.sendTitle("§3§l" + TextBeautifier.beautify("noxet"), "§eCaptcha System", 0, 120, 0);

        new ClearChat().send(player);
        new Message("§eHello! Before we let you in, please do this little captcha test.").send(player);

        int delay = 5;

        bukkitTasks.add(new BukkitRunnable() {
            @Override
            public void run() {
                new ClearChat().send(player);
                new Message("§bYou will hear a few sounds. Every time, you should answer by clicking the icon of what you heard.").send(player);
            }
        }.runTaskLater(NoxetServer.getPlugin(), 20 * delay));

        delay += 6;

        bukkitTasks.add(new BukkitRunnable() {
            @Override
            public void run() {
                new ClearChat().send(player);
                new Message("§9§oUnable to hear sounds? Enable Minecraft Subtitles in the sound options.").send(player);
            }
        }.runTaskLater(NoxetServer.getPlugin(), 20 * delay));

        delay += 5;

        bukkitTasks.add(new BukkitRunnable() {
            @Override
            public void run() {
                nextQuestion(0);
            }
        }.runTaskLater(NoxetServer.getPlugin(), 20 * delay));
    }

    private void nextQuestion(int index) {
        lastQuestion = index;

        Random random = new Random();
        List<CaptchaSound> captchaSounds = new ArrayList<>();

        for(int i = 0; i < answersPerQuestion; i++) {
            CaptchaSound suggestSound;

            do {
                suggestSound = CaptchaSound.values()[random.nextInt(CaptchaSound.values().length)];
            } while(captchaSounds.contains(suggestSound));

            captchaSounds.add(suggestSound);
        }

        correctAnswer = captchaSounds.get(random.nextInt(captchaSounds.size()));

        player.sendTitle("§d§lLISTEN", "§3What do you hear?", 0, 20 * 2, 0);

        player.playSound(player.getLocation(), correctAnswer.getSound(), 1, 1);

        bukkitTasks.add(new BukkitRunnable() {
            @Override
            public void run() {
                new CaptchaSelectionMenu(captchaSounds, index + 1, totalQuestions, captchaSound -> chooseAnswer(captchaSound)).openInventory(player);

                new ClearChat().send(player);
                new Message("§eClick what sound you heard from the menu.").send(player);
            }
        }.runTaskLater(NoxetServer.getPlugin(), 20 * 2));

        timeoutTask = new BukkitRunnable() {
            @Override
            public void run() {
                stop();
                player.kickPlayer("§cYou did not respond in time.");
            }
        }.runTaskLater(NoxetServer.getPlugin(), 20 * 20);
    }

    public void chooseAnswer(CaptchaSound answer) {
        if(correctAnswer == null) {
            new Message("§cPlease wait!").send(player);
            return;
        }

        if(answer == correctAnswer) {
            correctAnswer = null;

            new ClearChat().send(player);

            timeoutTask.cancel();

            player.stopAllSounds();
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1, 0.5f);

            if(lastQuestion < totalQuestions - 1) {
                for(int i = 60; i > 0; i -= 20) {
                    int finalI = i;
                    bukkitTasks.add(new BukkitRunnable() {
                        @Override
                        public void run() {
                            player.sendTitle("§2§lCORRECT", "§a" + (lastQuestion != totalQuestions - 2 ? "Another" : "Last") + " sound in §f" + finalI / 20 + "§as...", 0, 40, 0);
                        }
                    }.runTaskLater(NoxetServer.getPlugin(), 60 - i));
                }

                bukkitTasks.add(new BukkitRunnable() {
                    @Override
                    public void run() {
                        nextQuestion(lastQuestion + 1);
                    }
                }.runTaskLater(NoxetServer.getPlugin(), 60));
            } else {
                finish();
            }

            return;
        }

        stop();
        player.kickPlayer("§cWrong answer! Try again.");
    }

    public void finish() {
        stop();

        new Message("§aYou answered correctly and will now continue to the server.").send(player);

        player.sendTitle("§a§lDone", "§eYou seem human enough.", 0, 60, 0);

        new PlayerDataManager(player).set(PlayerDataManager.Attribute.HAS_DONE_CAPTCHA, true).save();

        new BukkitRunnable() {
            @Override
            public void run() {
                PlayerState.prepareDefault(player);
                if(player.isOnline())
                    goToHub(player);
            }
        }.runTaskLater(NoxetServer.getPlugin(), 60);
    }

    public void stop() {
        captchaInstances.remove(this);

        freezer.unfreeze(player);

        for(BukkitTask bukkitTask : bukkitTasks)
            bukkitTask.cancel();

        RealmManager.migratingPlayers.remove(player);

        getWorld().setBlockData(assignedLocation.clone().subtract(0, 1, 0), Material.AIR.createBlockData()); // Remove standing block.
    }

    public static World getWorld() {
        WorldCreator worldCreator = new WorldCreator("noxet_void_world");

        worldCreator.generator(new ChunkGenerator() {
            @Override
            @SuppressWarnings("all")
            public ChunkData generateChunkData(World world, Random random, int x, int z, BiomeGrid biome) {
                return createChunkData(world);
            }
        });

        return worldCreator.createWorld();
    }

    public static Captcha getPlayerCaptcha(Player player) {
        for(Captcha captcha : captchaInstances)
            if(captcha.getPlayer() == player)
                return captcha;
        return null;
    }

    public static boolean isPlayerDoingCaptcha(Player player) {
        return getPlayerCaptcha(player) != null;
    }

    /**
     * Stops a player's captcha process, if active.
     * @param player The player whose captcha to abort
     */
    public static void stopPlayerCaptcha(Player player) {
        Captcha captcha = getPlayerCaptcha(player);
        if(captcha != null)
            captcha.stop();
    }
}
