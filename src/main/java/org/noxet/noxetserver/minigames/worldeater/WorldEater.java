package org.noxet.noxetserver.minigames.worldeater;

import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.WorldInfo;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;
import org.bukkit.util.Consumer;
import org.noxet.noxetserver.NoxetServer;
import org.noxet.noxetserver.menus.inventory.WorldEaterTeamSelectionMenu;
import org.noxet.noxetserver.messaging.ActionBarMessage;
import org.noxet.noxetserver.messaging.Message;
import org.noxet.noxetserver.minigames.GameDefinition;
import org.noxet.noxetserver.minigames.MiniGameController;
import org.noxet.noxetserver.playerstate.PlayerState;
import org.noxet.noxetserver.util.FancyTimeConverter;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class WorldEater extends MiniGameController {
    protected final Set<Player> hiders, frozenPlayers;
    private Team seekersTeam;
    private Team hidersTeam;
    private Scoreboard scoreboard;
    private static final String cacheWorldName = "WORLDEATER_CACHE";

    @Override
    public void handlePreStart() {
        gameBuilder();
    }

    @Override
    public void handleStart() {
    }

    @Override
    public void handlePlayerJoin(Player player) {

    }

    @Override
    public void handlePlayerLeave(Player player) {
        if(isPlaying()) {
            if(isHider(player)) { // Hider left.
                hiders.remove(player);

                if(hiders.isEmpty()) { // No hider remains.
                    sendGameMessage(new Message("§cThere is no hider remaining, so the game is over."));
                    stop(false);
                }
            } else if(getPlayers().size() == hiders.size()) { // Only hiders remain.
                sendGameMessage(new Message("§cThere is no seeker remaining, so the game is over."));
                stop(true);
            } else if(getPlayers().size() == 1) { // Only 1 player remain.
                sendGameMessage(new Message("§cEverybody else quit. The game is over. :("));
                stop(isHider(getPlayers().toArray(new Player[0])[0]));
            }
        }
    }

    @Override
    public int handleSoftStop() {
        return 0;
    }

    @Override
    public void handlePostStop() {

    }

    @Override
    public DeathContract handleDeath(Player player) {
        return null;
    }

    @Override
    public Location getSpawnLocation() {
        return getAppropriateSpawnLocation(getCenterTopLocation());
    }

    private int timeLeft;
    private final List<GameEvent> events;

    private enum GameEvent {
        METEOR_RAIN("Meteor rain"),
        VISIBLE_HIDERS("Hiders are exposed"),
        DRILLING("Drilling"),
        SHRINKING_WORLD_BORDER("World border is shrinking"),
        EXPLODING_HORSES("Exploding horses are incoming");

        public final String eventName;

        GameEvent(String eventName) {
            this.eventName = eventName;
        }
    }

    public WorldEater() {
        super(GameDefinition.WORLD_EATER);

        hiders = new HashSet<>();
        frozenPlayers = new HashSet<>();
        events = new ArrayList<>();
    }

    private void gameBuilder() {
        sendGameMessage(new Message("Preparing game..."));

        World normalWorld;

        sendGameMessage(new Message("Loading normal world..."));

        WorldCreator normalWorldCreator = new WorldCreator(cacheWorldName);
        normalWorldCreator.type(WorldType.NORMAL);
        normalWorldCreator.generateStructures(true);
        normalWorldCreator.biomeProvider(new BiomeProvider() {
            @SuppressWarnings("all")
            @Override
            public Biome getBiome(WorldInfo worldInfo, int i, int i1, int i2) {
                return Biome.DARK_FOREST;
            }

            @SuppressWarnings("all")
            @Override
            public List<Biome> getBiomes(WorldInfo worldInfo) {
                List<Biome> result = new ArrayList<>();
                result.add(Biome.DARK_FOREST);
                return result;
            }
        });

        normalWorld = normalWorldCreator.createWorld();
        assert normalWorld != null;

        sendGameMessage(new Message("Setting up void world..."));

        getWorkingWorld().setPVP(false);

        // Reset world

        getWorkingWorld().setTime(0);
        getWorkingWorld().setClearWeatherDuration(20 * 60 * 30);

        // Clone chunk

        Random random = new Random();

        int chunkX = 0,
                chunkY = 0,
                chunkTries = 0;
        List<Integer> chunkCoordinate = new ArrayList<>();
        Chunk normalChunk = null;

        File badChunksFile = new File(NoxetServer.getPlugin().getPluginDirectory(), "worldEaterBadChunks.yml");
        YamlConfiguration badChunksConfig = YamlConfiguration.loadConfiguration(badChunksFile);

        @SuppressWarnings("unchecked")
        List<List<Integer>> badChunks = (List<List<Integer>>) badChunksConfig.getList("chunks");

        if(badChunks == null)
            badChunks = new ArrayList<>();

        sendGameMessage(new Message("Finding a good chunk..."));

        while(true) {
            if(chunkTries > 150) {
                sendGameMessage(new Message("Too many attempts! Gave up trying to find a good chunk."));
                break;
            }

            chunkCoordinate.clear();
            chunkCoordinate.add(chunkX);
            chunkCoordinate.add(chunkY);

            boolean registeredBadChunk = false;

            for(List<Integer> badChunkCoordinate : badChunks)
                if(badChunkCoordinate.equals(chunkCoordinate)) {
                    registeredBadChunk = true;
                    break;
                }

            if(registeredBadChunk || isChunkFlooded(normalWorld.getChunkAt(chunkX, chunkY))) {
                badChunks.add(new ArrayList<>(chunkCoordinate));

                chunkX = random.nextInt(0, 100);
                chunkY = random.nextInt(0, 100);
                chunkTries++;
            } else {
                normalChunk = normalWorld.getChunkAt(chunkX, chunkY);
                break;
            }
        }

        badChunks.add(chunkCoordinate);

        badChunksConfig.set("chunks", badChunks);

        try {
            badChunksConfig.save(badChunksFile);
        } catch(IOException e) {
            throw new RuntimeException(e);
        }

        Chunk chunk = getWorkingWorld().getChunkAt(0, 0);

        assert normalChunk != null;

        for(int x = 0; x < 16; x++)
            for(int z = 0; z < 16; z++)
                for(int y = getWorkingWorld().getMinHeight(); y < getWorkingWorld().getMaxHeight(); y++) {
                    Material sourceMaterial = normalChunk.getBlock(x, y, z).getType();
                    if(!sourceMaterial.isAir())
                        chunk.getBlock(x, y, z).setType(sourceMaterial);
                }

        for(int i = 0; i < 4; i++)
            spawnEntityInNaturalHabitat(EntityType.COW);

        for(int i = 0; i < 3; i++)
            spawnEntityInNaturalHabitat(EntityType.SHEEP);

        for(int i = 0; i < 2; i++)
            spawnEntityInNaturalHabitat(EntityType.CHICKEN);

        Scoreboard mainScoreboard = Objects.requireNonNull(Bukkit.getScoreboardManager()).getMainScoreboard();

        seekersTeam = mainScoreboard.getTeam("seekers");
        hidersTeam = mainScoreboard.getTeam("hiders");

        if(seekersTeam == null)
            seekersTeam = mainScoreboard.registerNewTeam("seekers");

        if(hidersTeam == null)
            hidersTeam = mainScoreboard.registerNewTeam("hiders");

        seekersTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.FOR_OTHER_TEAMS);
        seekersTeam.setCanSeeFriendlyInvisibles(false);
        seekersTeam.setAllowFriendlyFire(false);
        seekersTeam.setPrefix("§4§l[ §c§lSEEKER §4§l] ");
        seekersTeam.setColor(ChatColor.RED);

        hidersTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.FOR_OTHER_TEAMS);
        hidersTeam.setCanSeeFriendlyInvisibles(false);
        hidersTeam.setAllowFriendlyFire(false);
        hidersTeam.setPrefix("§2§l[ §a§lHIDER §2§l] ");
        hidersTeam.setColor(ChatColor.GREEN);

        scoreboard = createGameScoreboard();

        WorldEaterTeamSelectionMenu teamSelectionMenu = new WorldEaterTeamSelectionMenu(this, 40, menu -> {
            hiders.addAll(menu.getHiders());

            if(hiders.isEmpty()) {
                sendGameMessage(new Message("No one wanted to play as a hider! Picking a random hider."));
                hiders.add(getRandomPlayer());
            } else if(getPlayers().size() == hiders.size()) {
                sendGameMessage(new Message("No one wanted to play as a seeker! Picking a random seeker."));
                hiders.remove(getRandomPlayer());
            }

            gamePlay(chunk);
        });

        forEachPlayer(player -> {
            player.teleport(getSpawnLocation());
            PlayerState.prepareIdle(player, true);

            teamSelectionMenu.openInventory(player);

            player.setScoreboard(scoreboard);
        });

        frozenPlayers.addAll(getPlayers());
    }

    private void gamePlay(Chunk chunk) {
        sendGameMessage(new Message("§aHiders are..."));

        forEachPlayer(player -> PlayerState.prepareIdle(player, true));

        forEachSeeker(seeker -> seeker.sendTitle("§c§lSEEKER", "§eFind and eliminate the hiders.", 0, 20 * 5, 0));
        forEachHider(hider -> {
            hider.sendTitle("§a§lHIDER", "§eEndure the seekers attempts to kill you.", 0, 20 * 5, 0);
            sendGameMessage(new Message(" - §a§l" + hider.getName()));
        });

        sendGameMessage(new Message("§eGet ready! Game starts in §c5§e seconds..."));

        addTask(new BukkitRunnable() {
            @Override
            public void run() {
                frozenPlayers.clear();

                sendGameMessage(new Message("§eHiders are given a head start."));

                final ArrayList<BukkitTask> seekerCircleTasks = new ArrayList<>();

                forEachSeeker(seeker -> {
                    PlayerState.prepareIdle(seeker, true);

                    Location center = getCenterTopLocation().add(0, 20, 0);

                    double speed = 0.1;

                    final double[] angle = {0};

                    BukkitTask moveTask = Bukkit.getScheduler().runTaskTimer(NoxetServer.getPlugin(), () -> {
                        if(!seeker.isFlying())
                            seeker.setFlying(true);

                        double radians = Math.toRadians(angle[0]);

                        center.setYaw((float) Math.toDegrees(Math.atan2(-Math.cos(radians), Math.sin(radians))));
                        center.setPitch(90);
                        seeker.teleport(center);

                        angle[0] += speed;
                        angle[0] %= 360;
                    }, 0, 1);

                    addTask(moveTask);
                    seekerCircleTasks.add(moveTask);

                    seekersTeam.addEntry(seeker.getName());
                });

                forEachHider(hider -> {
                    preparePlayer(hider);

                    hider.playSound(hider, Sound.BLOCK_NOTE_BLOCK_BASS, 1, 0.5f);
                    hider.sendTitle("§c§lHURRY UP!", "§ePrepare and reach §3shelter§e fast!", 5, 20 * 5, 10);

                    hidersTeam.addEntry(hider.getName());
                });


                int secondsToRelease = 2 * 60;

                for(int i = secondsToRelease; i > 0; i--) {
                    int finalI = i;
                    addTask(new BukkitRunnable() {
                        @Override
                        public void run() {
                            String timeLeftString = FancyTimeConverter.deltaSecondsToFancyTime(finalI);

                            if(finalI % 2 == 0)
                                playGameSound(Sound.BLOCK_POINTED_DRIPSTONE_FALL, 0.6f, 2);

                            if(finalI % 10 == 0)
                                sendGameMessage(new Message("§eSeekers are released in " + timeLeftString + "."));

                            forEachSeeker(seeker -> seeker.sendTitle(timeLeftString, "§euntil released...", 0, 20 * 2, 0));
                            forEachHider(hider -> new ActionBarMessage("§8§l[ §" + (finalI % 2 == 0 ? "4" : "f") + "§l! §8§l] §bReleasing seekers in " + timeLeftString).send(hider));
                        }
                    }.runTaskLater(NoxetServer.getPlugin(), 20L * (secondsToRelease - i)));
                }

                addTask(new BukkitRunnable() {
                    @Override
                    public void run() {
                        for(BukkitTask task : seekerCircleTasks)
                            task.cancel();

                        frozenPlayers.clear();

                        sendGameMessage(new Message("§c§lSEEKERS HAVE BEEN RELEASED!"));

                        getWorkingWorld().setPVP(true);

                        playGameSound(Sound.BLOCK_ANVIL_LAND, 2, 2);

                        forEachSeeker(seeker -> {
                            preparePlayer(seeker);
                            seeker.resetTitle();
                        });

                        forEachHider(hider -> new ActionBarMessage("§c§lSEEKERS RELEASED!").send(hider));

                        for(int i = 0; i < 4; i++)
                            spawnEntityInNaturalHabitat(EntityType.COW);

                        for(int i = 0; i < 3; i++)
                            spawnEntityInNaturalHabitat(EntityType.SHEEP);

                        for(int i = 0; i < 2; i++)
                            spawnEntityInNaturalHabitat(EntityType.CHICKEN);

                        sendGameMessage(new Message("§c(!) Starting from the top of the island to the bottom, each layer of blocks will be removed at an exponential rate."));

                        int startY = 0;

                        for(int x = 0; x < 16; x++)
                            for(int z = 0; z < 16; z++)
                                startY = Math.max(startY, getWorkingWorld().getHighestBlockYAt(x, z));

                        long progress = 0;

                        for(int y = startY; y > -50; y--) {
                            int finalY = y;
                            progress += 20L * 30 * Math.pow(0.983D, 1 + startY - y);
                            addTask(new BukkitRunnable() {
                                @Override
                                public void run() {
                                    playGameSound(Sound.BLOCK_BAMBOO_WOOD_PRESSURE_PLATE_CLICK_OFF, 1, 1);

                                    for(int x = 0; x < 16; x++)
                                        for(int z = 0; z < 16; z++)
                                            getWorkingWorld().setBlockData(chunk.getBlock(x, finalY, z).getLocation(), Material.AIR.createBlockData());
                                }
                            }.runTaskLater(NoxetServer.getPlugin(), progress));
                        }

                        sendGameMessage(new Message("§eIf the hiders survive until the game is over, they win. Otherwise the seekers win."));

                        timeLeft = 30 * 60;

                        for(int i = timeLeft / 60; i >= 0; i--) {
                            int finalI = i;
                            addTask(new BukkitRunnable() {
                                @Override
                                public void run() {
                                    if(finalI > 0) {
                                        if(finalI % 5 == 0 || finalI < 10)
                                            sendGameMessage(new Message("§eThe game has §c" + finalI + "§e minutes remaining."));

                                        switch(finalI) { // Timed events
                                            case 20:
                                                events.add(GameEvent.METEOR_RAIN);

                                                playGameSound(Sound.ITEM_GOAT_HORN_SOUND_3, 1, 2);
                                                sendGameMessage(new Message("§c§lMETEOR RAIN! §cHead to shelter!"));

                                                final int meteorAmount = 10;
                                                for(int i = 0; i < meteorAmount; i++) {
                                                    boolean isLast = i == meteorAmount - 1;
                                                    addTask(new BukkitRunnable() {
                                                        @Override
                                                        public void run() {
                                                            Player meteorTarget = getRandomPlayer(true);

                                                            meteorTarget.playSound(meteorTarget, Sound.ITEM_GOAT_HORN_SOUND_0, 1, 0.5f);

                                                            Location targetLocation = meteorTarget.getLocation();

                                                            Random random = new Random();

                                                            Location meteorStart = targetLocation.clone();
                                                            meteorStart.add(random.nextInt(-50, 50), random.nextInt(50, 100), random.nextInt(-50, 50));

                                                            Fireball meteor = getWorkingWorld().spawn(meteorStart, Fireball.class);

                                                            meteor.setIsIncendiary(true);
                                                            meteor.setYield(8);

                                                            meteor.setDirection(targetLocation.toVector().subtract(meteorStart.toVector()));

                                                            if(isLast)
                                                                events.remove(GameEvent.METEOR_RAIN);
                                                        }
                                                    }.runTaskLater(NoxetServer.getPlugin(), 20 * (i + 1) * 15));
                                                }

                                                break;
                                            case 22:
                                            case 15:
                                            case 8:
                                                events.add(GameEvent.VISIBLE_HIDERS);

                                                playGameSound(Sound.ITEM_GOAT_HORN_SOUND_7, 1, 0.8f);
                                                sendGameMessage(new Message("§c§lALERT! §eHiders are now visible for 10 seconds!"));

                                                forEachHider(hider -> {
                                                    hider.sendTitle("§c§lEXPOSED!", "§eYour location is now visible.", 5, 20 * 10, 5);
                                                    hider.addPotionEffect(
                                                            new PotionEffect(
                                                                    PotionEffectType.GLOWING, 20 * 10, 10, true, false, false
                                                            )
                                                    );
                                                });

                                                addTask(new BukkitRunnable() {
                                                    @Override
                                                    public void run() {
                                                        events.remove(GameEvent.VISIBLE_HIDERS);
                                                    }
                                                }.runTaskLater(NoxetServer.getPlugin(), 20 * 10));
                                                break;
                                            case 12:
                                                events.add(GameEvent.DRILLING);

                                                playGameSound(Sound.ITEM_GOAT_HORN_SOUND_4, 1, 2f);
                                                sendGameMessage(new Message("§c§lDRILLING! §cDrills will now be performed randomly. A whole Y-axis will be drilled down into void!"));
                                                Random random = new Random();

                                                final int drillHoles = 15;
                                                for(int i = 0; i < drillHoles; i++) {
                                                    boolean isLast = i == drillHoles - 1;

                                                    addTask(new BukkitRunnable() {
                                                        @Override
                                                        public void run() {
                                                            Location drillLocation = new Location(getWorkingWorld(), random.nextInt(0, 16), 0, random.nextInt(0, 16));
                                                            int yMax = getWorkingWorld().getMaxHeight(), yMin = getWorkingWorld().getMinHeight();

                                                            for(int y = yMin; y < yMax; y++) {
                                                                Location drillBlock = drillLocation.clone();
                                                                drillBlock.setY(y);

                                                                getWorkingWorld().spawnParticle(Particle.SWEEP_ATTACK, drillBlock, 3);
                                                                int finalY = y;
                                                                boolean isLast2 = isLast && y == yMin + 1;

                                                                addTask(new BukkitRunnable() {
                                                                    @Override
                                                                    public void run() {
                                                                        if(finalY % 2 == 0)
                                                                            getWorkingWorld().playSound(drillBlock, Sound.BLOCK_BAMBOO_BREAK, 1, 2f);

                                                                        getWorkingWorld().spawnParticle(Particle.SWEEP_ATTACK, drillBlock, 5);
                                                                        drillBlock.getBlock().setBlockData(Material.AIR.createBlockData(), false);

                                                                        if(isLast2)
                                                                            events.remove(GameEvent.DRILLING);
                                                                    }
                                                                }.runTaskLater(NoxetServer.getPlugin(), 2L * (yMax - y)));
                                                            }
                                                        }
                                                    }.runTaskLater(NoxetServer.getPlugin(), 20 * 15 * (i + 1)));
                                                }

                                                break;
                                            case 10:
                                                events.add(GameEvent.SHRINKING_WORLD_BORDER);

                                                playGameSound(Sound.ITEM_GOAT_HORN_SOUND_6, 1, 2);
                                                sendGameMessage(new Message("§eThe world border will shrink in §c30§e seconds!"));

                                                addTask(new BukkitRunnable() {
                                                    @Override
                                                    public void run() {
                                                        playGameSound(Sound.ITEM_GOAT_HORN_SOUND_6, 1, 0.5f);

                                                        getWorkingWorld().getWorldBorder().setSize(32);
                                                        getWorkingWorld().getWorldBorder().setWarningTime(20);
                                                        getWorkingWorld().getWorldBorder().setCenter(getSpawnLocation());
                                                        sendGameMessage(new Message("§eWorld border has shrunk!"));

                                                        events.remove(GameEvent.SHRINKING_WORLD_BORDER);
                                                    }
                                                }.runTaskLater(NoxetServer.getPlugin(), 20 * 30));
                                                break;
                                            case 5:
                                                events.add(GameEvent.EXPLODING_HORSES);

                                                sendGameMessage(new Message("§8<§k-§8> §4§lSUDDEN DEATH! §cExploding horses will appear. They may be killed with a single hit, but - if not - they will put you down."));

                                                playGameSound(Sound.ENTITY_HORSE_ANGRY, 5, 5);

                                                final int horseCount = 20;

                                                for(int i = 0; i < horseCount; i++) {
                                                    boolean isLast = i == horseCount - 1;

                                                    addTask(new BukkitRunnable() {
                                                        @Override
                                                        public void run() {
                                                            if(Math.random() * 10 < 3) {
                                                                Player unluckyPlayer = getRandomPlayer();
                                                                unluckyPlayer.playSound(unluckyPlayer, Sound.ENTITY_HORSE_ANGRY, 6, 6);

                                                                Horse horse = (Horse) getWorkingWorld().spawnEntity(unluckyPlayer.getLocation(), EntityType.HORSE);

                                                                horse.setVisualFire(true);
                                                                horse.setHealth(0.5);

                                                                addTask(new BukkitRunnable() {
                                                                    @Override
                                                                    public void run() {
                                                                        if(!horse.isDead()) {
                                                                            getWorkingWorld().playSound(horse.getLocation(), Sound.ENTITY_GHAST_SCREAM, 1, 0.5f);
                                                                            horse.remove();
                                                                            getWorkingWorld().createExplosion(horse.getLocation(), 12);
                                                                        }

                                                                        if(isLast)
                                                                            events.remove(GameEvent.EXPLODING_HORSES);
                                                                    }
                                                                }.runTaskLater(NoxetServer.getPlugin(), 20 * 4));
                                                            }
                                                        }
                                                    }.runTaskLater(NoxetServer.getPlugin(), 20 * 3 * i));
                                                }
                                                break;
                                            case 1:
                                                playGameSound(Sound.ITEM_GOAT_HORN_SOUND_7, 1, 0.5f);
                                                sendGameMessage(new Message("§c§lALERT! §eEVERYONE are now visible!"));

                                                forEachPlayer(player -> {
                                                    player.sendTitle("§c§lEXPOSED!", "§eEveryone can now see everyone.", 5, 20 * 5, 5);
                                                    player.addPotionEffect(
                                                            new PotionEffect(
                                                                    PotionEffectType.GLOWING, 20 * 60, 10, true, false, false
                                                            )
                                                    );
                                                });

                                                break;
                                        }
                                    } else {
                                        sendGameMessage(new Message("§aTime has gone out! Hiders win."));
                                        stop(true);
                                    }
                                }
                            }.runTaskLater(NoxetServer.getPlugin(), 20L * 60 * (timeLeft - i)));
                        }

                        addTask(new BukkitRunnable() {
                            @Override
                            public void run() {
                                updateScoreboard();
                                timeLeft--;
                            }
                        }.runTaskTimer(NoxetServer.getPlugin(), 40, 20));
                    }
                }.runTaskLater(NoxetServer.getPlugin(), 20 * secondsToRelease));
            }
        }.runTaskLater(NoxetServer.getPlugin(), 20 * 5));
    }

    protected void stopHard(boolean wait) {
        if(scoreboard != null)
            scoreboard.getObjectives().forEach(Objective::unregister);

        if(seekersTeam != null)
            for(String entry : seekersTeam.getEntries())
                seekersTeam.removeEntry(entry);

        if(hidersTeam != null)
            for(String entry : hidersTeam.getEntries())
                hidersTeam.removeEntry(entry);
    }

    protected void stop(boolean hiderWins) {
        playGameSound(Sound.BLOCK_BELL_USE, 3, 3);

        forEachPlayer(player -> player.sendTitle(
                (!hiderWins && isSeeker(player)) || (hiderWins && isHider(player)) ?
                        "§a§lVictory!" : "§c§lLost!",
                !hiderWins ?
                        "§eSeekers won." : "§eHiders won.",
                0, 20 * 6, 0
        ));

        sendGameMessage(new Message("§aThe game has ended. Thanks for playing."));

        Random random = new Random();
        for(int i = 0; i < 10; i++) {
            double x = random.nextInt(-16, 16), z = random.nextInt(-16, 16);
            FireworkEffect effect = FireworkEffect.builder().flicker(false).trail(false).with(FireworkEffect.Type.STAR).withColor(Color.fromRGB(
                    random.nextInt(256),
                    random.nextInt(256),
                    random.nextInt(256)
            )).build();

            Firework firework = getWorkingWorld().spawn(new Location(getWorkingWorld(), x, getWorkingWorld().getHighestBlockYAt((int) x, (int) z) + random.nextInt(2, 10), z), Firework.class);

            FireworkMeta fireworkMeta = firework.getFireworkMeta();
            fireworkMeta.clearEffects();
            fireworkMeta.addEffect(effect);

            firework.setFireworkMeta(fireworkMeta);

            new BukkitRunnable() {
                public void run() {
                    firework.detonate();
                }
            }.runTaskLater(NoxetServer.getPlugin(), 20L * random.nextInt(1, 4));
        }
    }

    protected Location getCenterTopLocation() {
        return new Location(getWorkingWorld(), 8, getWorkingWorld().getHighestBlockYAt(8, 8), 8);
    }

    protected Location getAppropriateSpawnLocation(Location spawn) {
        while(true)
            if(spawn.getBlock().getType().isAir() || spawn.getBlock().getType().toString().endsWith("_LEAVES") || spawn.getBlock().getType().toString().endsWith("_MUSHROOM"))
                spawn.subtract(0, 1, 0); // Go down 1 block if air or leaf block.
            else if(spawn.getBlock().getType().toString().endsWith("_LOG"))
                spawn.add(1, 0, 0); // Go x++ if wood log block.
            else break;

        return spawn.add(0, 2, 0);
    }

    private Player getRandomPlayer() {
        return getRandomPlayer(false);
    }

    private Player getRandomPlayer(boolean notFrozen) {
        if(!notFrozen)
            return getPlayers().toArray(new Player[0])[(int) (getPlayers().size() * Math.random())];

        ArrayList<Player> availablePlayers = new ArrayList<>(getPlayers());
        availablePlayers.removeAll(frozenPlayers);

        if(availablePlayers.size() == 0)
            return getRandomPlayer();

        return availablePlayers.toArray(new Player[0])[(int) (availablePlayers.size() * Math.random())];
    }

    private static boolean hasLiquidOnTop(World world, int x, int z) {
        for(int y = world.getMaxHeight(); y >= world.getMinHeight(); y--)
            if(!world.getBlockAt(x, y, z).getType().isAir()) // Is not air?
                return world.getBlockAt(x, y, z).isLiquid(); // Highest block: liquid or not.
        return false;
    }

    private static boolean isChunkFlooded(Chunk chunk) {
        int floodPoints = 0;

        for(int x = 0; x < 16; x++)
            for(int z = 0; z < 16; z++)
                if(hasLiquidOnTop(chunk.getWorld(), (chunk.getX() << 4) + x, (chunk.getZ() << 4) + z)) {
                    floodPoints++;
                    if(floodPoints > 10)
                        return true;
                }

        return false;
    }

    private void spawnEntityInNaturalHabitat(EntityType type) {
        Location spawn = getSpawnLocation();

        Random random = new Random();

        spawn.setX(random.nextInt(3, 14));
        spawn.setZ(random.nextInt(3, 14));
        spawn.setY(getWorkingWorld().getHighestBlockYAt((int) spawn.getX(), (int) spawn.getZ()));

        getWorkingWorld().spawnEntity(getAppropriateSpawnLocation(spawn), type);
    }

    protected void seekerRespawn(Player seeker) {
        Location respawnLocation = seeker.getLastDeathLocation();
        if(respawnLocation != null && respawnLocation.getY() > getWorkingWorld().getMinHeight())
            seeker.teleport(seeker.getLastDeathLocation()); // Return seeker to death location.

        frozenPlayers.add(seeker);
        seeker.setGameMode(GameMode.SPECTATOR);

        seeker.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 10, 10, true, false));

        for(int i = 10; i > 0; i--) {
            int finalI = i;
            addTask(new BukkitRunnable() {
                @Override
                public void run() {
                    seeker.sendTitle("§c" + finalI, "§euntil you respawn...", 0, 20 * 2, 0);
                }
            }.runTaskLater(NoxetServer.getPlugin(), 20L * (10 - i)));
        }

        addTask(new BukkitRunnable() {
            @Override
            public void run() {
                seeker.setGameMode(GameMode.SURVIVAL);
                frozenPlayers.remove(seeker);

                PlayerState.prepareNormal(seeker, false);

                seeker.teleport(getSpawnLocation());
            }
        }.runTaskLater(NoxetServer.getPlugin(), 20 * 10));
    }

    private static Scoreboard createGameScoreboard() {
        Scoreboard scoreboard = Objects.requireNonNull(Bukkit.getScoreboardManager()).getNewScoreboard();

        Objective objective = scoreboard.registerNewObjective("worldeater_scoreboard", Criteria.DUMMY, "§6§lWORLD§2§lEATER");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        return scoreboard;
    }

    private static void setObjectiveLines(Objective objective, String[] lines) {
        for(String score : Objects.requireNonNull(objective.getScoreboard()).getEntries())
            objective.getScoreboard().resetScores(score);

        int i = 0;
        for(String line : lines)
            objective.getScore(line).setScore(lines.length - i++);
    }

    private void updateScoreboard() {
        setObjectiveLines(scoreboard.getObjectives().toArray(new Objective[0])[0], new String[] {
                "§eTime remaining: " + getFancyTimeLeft(timeLeft),
                "§e" + getPlayers().size() + " playing:",
                "§c" + (getPlayers().size() - hiders.size()) + "§e seeking",
                "§a" + hiders.size() + "§e hiding",
                "§7---",
                "§7" + getSpectators().size() + "§8 spectating",
                "§7---",
                events.size() == 0 ? "§7No event" : events.get(0).eventName
        });
    }

    private static String getFancyTimeLeft(int countdown) {
        return (countdown >= 60 ? "§c" + (countdown / 60) + "§em " : "") + "§c" + countdown % 60 + "§es";
    }

    public boolean isSeeker(Player player) {
        return isPlayer(player) && !isHider(player);
    }

    public boolean isHider(Player player) {
        return hiders.contains(player);
    }

    public void forEachPlayer(Consumer<Player> playerConsumer) {
        for(Player player : getPlayers())
            playerConsumer.accept(player);
    }

    public void forEachSeeker(Consumer<Player> seekerConsumer) {
        for(Player player : getPlayers())
            if(!isHider(player))
                seekerConsumer.accept(player);
    }

    public void forEachHider(Consumer<Player> hiderConsumer) {
        for(Player hider : hiders)
            hiderConsumer.accept(hider);
    }
}
