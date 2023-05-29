package org.noxet.noxetserver;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.noxet.noxetserver.messaging.ClearChat;
import org.noxet.noxetserver.messaging.NoxetMessage;
import org.noxet.noxetserver.messaging.TextBeautifier;
import org.noxet.noxetserver.playerstate.PlayerState;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.noxet.noxetserver.RealmManager.goToHub;

public class Captcha {

    private enum CaptchaSound {
        DOG("Wolf", Sound.ENTITY_WOLF_HURT),
        PIG("Pig", Sound.ENTITY_PIG_AMBIENT),
        SHEEP("Sheep", Sound.ENTITY_SHEEP_AMBIENT),
        CREEPER("Creeper", Sound.ENTITY_CREEPER_PRIMED),
        GLASS("Glass", Sound.BLOCK_GLASS_BREAK),
        EXPLOSION("Explosion", Sound.ENTITY_GENERIC_EXPLODE),
        PARROT("Parrot", Sound.ENTITY_PARROT_AMBIENT),
        SHULKER_BOX("Shulker Box", Sound.BLOCK_SHULKER_BOX_OPEN),
        ENDER_DRAGON("Ender Dragon", Sound.ENTITY_ENDER_DRAGON_GROWL),
        WITHER("Wither", Sound.ENTITY_WITHER_AMBIENT),
        CHEST("Chest", Sound.BLOCK_CHEST_OPEN),
        ENDERMAN("Enderman", Sound.ENTITY_ENDERMAN_AMBIENT),
        SILVERFISH("Silverfish", Sound.ENTITY_SILVERFISH_AMBIENT),
        DEEPSLATE("Deepslate", Sound.BLOCK_DEEPSLATE_BREAK),
        FIRE("Fire", Sound.BLOCK_FIRE_EXTINGUISH),
        ANVIL("Anvil", Sound.BLOCK_ANVIL_USE),
        HORSE("Horse", Sound.ENTITY_HORSE_AMBIENT),
        ARMOR("Armor", Sound.ITEM_ARMOR_EQUIP_GENERIC),
        GRASS("Grass", Sound.BLOCK_GRASS_BREAK),
        END_PORTAL("End Portal Frame", Sound.BLOCK_END_PORTAL_FRAME_FILL),
        NETHER_PORTAL("Nether Portal", Sound.BLOCK_PORTAL_AMBIENT),
        WOOL("Wool", Sound.BLOCK_WOOL_BREAK),
        DOLPHIN("Dolphin", Sound.ENTITY_DOLPHIN_AMBIENT),
        BURP("Burping", Sound.ENTITY_PLAYER_BURP),
        FALL("Fall", Sound.ENTITY_PLAYER_BIG_FALL),
        ZOMBIE("Zombie", Sound.ENTITY_ZOMBIE_AMBIENT),
        SKELETON("Skeleton", Sound.ENTITY_SKELETON_AMBIENT),
        VILLAGER("Villager", Sound.ENTITY_VILLAGER_YES);

        private final String name;
        private final Sound sound;

        CaptchaSound(String name, Sound sound) {
            this.name = name;
            this.sound = sound;
        }

        public String getName() {
            return name;
        }

        public Sound getSound() {
            return sound;
        }
    }

    private static final List<Captcha> captchaInstances = new ArrayList<>();

    private final Player player;
    private final Location assignedLocation;
    private int lastQuestion;
    private int correctAnswer = -1;
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

        BukkitTask teleportTimer = new BukkitRunnable() {
            @Override
            public void run() {
                player.teleport(assignedLocation);
            }
        }.runTaskTimer(NoxetServer.getPlugin(), 0, 5);

        bukkitTasks.add(teleportTimer);

        player.sendTitle("§3§l" + TextBeautifier.beautify("noxet"), "§eCaptcha System", 0, 120, 0);

        bukkitTasks.add(new BukkitRunnable() {
            @Override
            public void run() {
                new NoxetMessage("§e§nBefore we let you in to the server, we must confirm that you are human.").send(player);
            }
        }.runTaskLater(NoxetServer.getPlugin(), 20));

        bukkitTasks.add(new BukkitRunnable() {
            @Override
            public void run() {
                new NoxetMessage("§bYou will hear a few sounds. For each sound, answer what you heard.").send(player);
            }
        }.runTaskLater(NoxetServer.getPlugin(), 20 * (1 + 7)));

        bukkitTasks.add(new BukkitRunnable() {
            @Override
            public void run() {
                new NoxetMessage("§9§oUnable to hear sounds? Enable Minecraft Subtitles in the sound options.").send(player);
            }
        }.runTaskLater(NoxetServer.getPlugin(), 20 * (1 + 7 + 5)));

        bukkitTasks.add(new BukkitRunnable() {
            @Override
            public void run() {
                nextQuestion(0);
            }
        }.runTaskLater(NoxetServer.getPlugin(), 20 * (1 + 7 + 5 + 5)));
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

        correctAnswer = random.nextInt(captchaSounds.size());

        new ClearChat().send(player);

        player.sendTitle("§e§l?§f§l?", "§7What sound did you hear? Click in chat to answer.", 20, 60, 10);

        new NoxetMessage("§9§lSOUND §f" + (index + 1) + " §7/ §f" + totalQuestions).send(player);

        new NoxetMessage("§7- - - - - - - - - - - -").send(player);

        int i = 0;
        for(CaptchaSound captchaSound : captchaSounds)
            new NoxetMessage().add("§e§l" + ++i + " §8§l| §b" + captchaSound.getName(), captchaSound.getName(), String.valueOf(i - 1)).send(player);

        player.playSound(player.getLocation(), captchaSounds.get(correctAnswer).getSound(), 1, 1);

        new NoxetMessage("§eWhat sound did you hear? Click the correct answer above.").send(player);

        timeoutTask = new BukkitRunnable() {
            @Override
            public void run() {
                stop();
                player.kickPlayer("§cYou did not respond in time.");
            }
        }.runTaskLater(NoxetServer.getPlugin(), 20 * 20);
    }

    public void chooseAnswer(int answer) {
        if(correctAnswer == -1) {
            new NoxetMessage("§cPlease wait!").send(player);
            return;
        }

        if(answer == correctAnswer) {
            correctAnswer = -1;

            new ClearChat().send(player);

            timeoutTask.cancel();

            player.stopAllSounds();
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1, 0.5f);

            if(lastQuestion < totalQuestions - 1) {
                player.sendTitle("§2§lCORRECT", "§fPrepare for a new sound.", 0, 80, 0);

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

        new NoxetMessage("§a§nYou answered correctly and will now continue to the Noxet.org server.").send(player);

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
