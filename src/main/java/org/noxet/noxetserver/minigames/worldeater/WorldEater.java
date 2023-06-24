package org.noxet.noxetserver.minigames.worldeater;

import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
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
import org.noxet.noxetserver.util.TextBeautifier;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class WorldEater extends MiniGameController {
    private final Set<Player> hiders;
    private Team seekersTeam;
    private Team hidersTeam;
    private Scoreboard scoreboard;
    private static final String cacheWorldName = "WORLDEATER_CACHE";

    /**
     * Used for accessing the game instance from other scopes.
     */
    private final WorldEater controllerInstance = this;

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
                    finish(GameResult.TIE);
                }
            } else if(getPlayers().size() == hiders.size()) { // Only hiders remain.
                sendGameMessage(new Message("§cThere is no seeker remaining, so the game is over."));
                finish(GameResult.TIE);
            } else if(getPlayers().size() == 1) { // Only 1 player remain.
                sendGameMessage(new Message("§cEverybody else quit. The game is over. :("));
                finish(GameResult.TIE);
            }
        }
    }

    @Override
    public void handlePlayerRemoved(Player player) {
        getFreezer().unfreeze(player);
    }

    @Override
    public int handleSoftStop() {
        return 20 * 15;
    }

    @Override
    public void handlePostStop() {
        if(scoreboard != null)
            scoreboard.getObjectives().forEach(Objective::unregister);

        if(seekersTeam != null)
            for(String entry : seekersTeam.getEntries())
                seekersTeam.removeEntry(entry);

        if(hidersTeam != null)
            for(String entry : hidersTeam.getEntries())
                hidersTeam.removeEntry(entry);
    }

    @Override
    public DeathContract handleDeath(Player player) {
        return isSeeker(player) ? DeathContract.SPECTATE : DeathContract.RESPAWN_SAME_LOCATION_KEEP_INVENTORY;
    }

    @Override
    public void handleRespawn(Player player) {
        if(!isSeeker(player))
            return;

        getFreezer().freeze(player);
        player.setGameMode(GameMode.SPECTATOR);

        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 10, 10, true, false));

        for(int i = 10; i > 0; i--) {
            int finalI = i;
            addTask(new BukkitRunnable() {
                @Override
                public void run() {
                    player.sendTitle("§c" + finalI, "§euntil you respawn...", 0, 20 * 2, 0);
                }
            }.runTaskLater(NoxetServer.getPlugin(), 20L * (10 - i)));
        }

        addTask(new BukkitRunnable() {
            @Override
            public void run() {
                player.setGameMode(GameMode.SURVIVAL);
                getFreezer().unfreeze(player);

                PlayerState.prepareNormal(player, false);

                player.teleport(getSpawnLocation());
            }
        }.runTaskLater(NoxetServer.getPlugin(), 20 * 10));
    }

    @Override
    public Location getSpawnLocation() {
        return getAppropriateSpawnLocation(getCenterTopLocation());
    }

    private int timeLeft;
    private final List<WorldEaterEvents.GameEvent> events;

    public WorldEater() {
        super(GameDefinition.WORLD_EATER);

        hiders = new HashSet<>();
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

        getFreezer().bulkFreeze(getPlayers());
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
                getFreezer().empty();

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

                        getFreezer().empty();

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
                                                events.add(WorldEaterEvents.GameEvent.METEOR_RAIN);
                                                WorldEaterEvents.meteorRain(controllerInstance);
                                                break;
                                            case 22:
                                            case 15:
                                            case 8:
                                                events.add(WorldEaterEvents.GameEvent.VISIBLE_HIDERS);
                                                WorldEaterEvents.visibleHiders(controllerInstance);
                                                break;
                                            case 12:
                                                events.add(WorldEaterEvents.GameEvent.DRILLING);
                                                WorldEaterEvents.drilling(controllerInstance);
                                                break;
                                            case 10:
                                                events.add(WorldEaterEvents.GameEvent.SHRINKING_WORLD_BORDER);
                                                WorldEaterEvents.shrinkingWorldBorder(controllerInstance);
                                                break;
                                            case 5:
                                                events.add(WorldEaterEvents.GameEvent.EXPLODING_HORSES);
                                                WorldEaterEvents.explodingHorses(controllerInstance);
                                                break;
                                            case 1:
                                                events.add(WorldEaterEvents.GameEvent.EVERYONE_VISIBLE);
                                                WorldEaterEvents.everyoneVisible(controllerInstance);
                                                break;
                                        }
                                    } else {
                                        sendGameMessage(new Message("§aTime has gone out! Hiders win."));
                                        finish(GameResult.HIDERS_WIN);
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

    public enum GameResult {
        SEEKERS_WIN, TIE, HIDERS_WIN
    }

    public boolean didPlayerWin(Player player, GameResult result) {
        return (isSeeker(player) && result == GameResult.SEEKERS_WIN) || (isHider(player) && result == GameResult.HIDERS_WIN);
    }

    private void finish(GameResult result) {
        playGameSound(Sound.BLOCK_BELL_USE, 3, 3);

        if(result != GameResult.TIE)
            sendGameMessage(new Message((result == GameResult.SEEKERS_WIN ? "Seekers" : "Hiders") + " won the game!"));
        else
            sendGameMessage(new Message("Tie! Nobody won the game."));

        forEachPlayer(player -> player.sendTitle(
                didPlayerWin(player, result) ?
                        "§a§l" + TextBeautifier.beautify("Victory", false) + "!" :
                        (result != GameResult.TIE ?
                                "§c§l" + TextBeautifier.beautify("Lost", false) + "!" :
                                "§e§l" + TextBeautifier.beautify("Tie", false) + "!"
                        ),
                result == GameResult.SEEKERS_WIN ?
                        "§eSeekers won." : (result == GameResult.HIDERS_WIN ? "§eHiders won." : "§7Nobody won."),
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

        softStop();
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

    public void removeEvent(WorldEaterEvents.GameEvent event) {
        events.remove(event);
    }
}
