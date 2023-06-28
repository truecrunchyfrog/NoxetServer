package org.noxet.noxetserver.minigames.worldeater;

import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.WorldInfo;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;
import org.noxet.noxetserver.NoxetServer;
import org.noxet.noxetserver.menus.ItemGenerator;
import org.noxet.noxetserver.menus.inventory.WorldEaterTeamSelectionMenu;
import org.noxet.noxetserver.messaging.ActionBarMessage;
import org.noxet.noxetserver.messaging.ErrorMessage;
import org.noxet.noxetserver.messaging.Message;
import org.noxet.noxetserver.minigames.GameDefinition;
import org.noxet.noxetserver.minigames.MiniGameController;
import org.noxet.noxetserver.playerstate.PlayerState;
import org.noxet.noxetserver.util.FancyTimeConverter;
import org.noxet.noxetserver.util.RegionBinder;
import org.noxet.noxetserver.util.TeamSet;
import org.noxet.noxetserver.util.TextBeautifier;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class WorldEater extends MiniGameController {
    private Team seekersTeam, hidersTeam;
    private final TeamSet teamSet = new TeamSet(getPlayers(), WorldEaterTeams.SEEKER, WorldEaterTeams.HIDER);
    private Scoreboard scoreboard;
    private GameResult result;
    private static final String cacheWorldName = "WORLDEATER_CACHE";

    /**
     * Used for accessing the game instance from other scopes.
     */
    private final WorldEater controllerInstance = this;

    @Override
    public void handlePreStart() {
        World normalWorld;

        WorldCreator normalWorldCreator = new WorldCreator(cacheWorldName);
        normalWorldCreator.type(WorldType.NORMAL);
        normalWorldCreator.generateStructures(true);
        normalWorldCreator.biomeProvider(new BiomeProvider() {
            @Override
            @SuppressWarnings("all")
            public Biome getBiome(WorldInfo worldInfo, int i, int i1, int i2) {
                return Biome.DARK_FOREST;
            }

            @Override
            @SuppressWarnings("all")
            public List<Biome> getBiomes(WorldInfo worldInfo) {
                List<Biome> result = new ArrayList<>();
                result.add(Biome.DARK_FOREST);
                return result;
            }
        });

        normalWorld = normalWorldCreator.createWorld();
        assert normalWorld != null;

        setPvpRule(false);

        // Clone chunk

        Random random = new Random();

        int chunkX = 0,
            chunkY = 0,
            chunkTries = 0;
        List<Integer> chunkCoordinate = new ArrayList<>();
        Chunk normalChunk = null;

        File badChunksFile = new File(NoxetServer.getPlugin().getPluginDirectory(), "world-eater-bad-chunks.yml");
        YamlConfiguration badChunksConfig = YamlConfiguration.loadConfiguration(badChunksFile);

        @SuppressWarnings("unchecked")
        List<List<Integer>> badChunks = (List<List<Integer>>) badChunksConfig.getList("chunks");

        if(badChunks == null)
            badChunks = new ArrayList<>();

        while(true) {
            if(chunkTries > 150) {
                sendGameMessage(new Message("Too many attempts! Gave up trying to find a good chunk."));
                break;
            }

            chunkCoordinate.clear();
            chunkCoordinate.addAll(Arrays.asList(chunkX, chunkY));

            boolean registeredBadChunk = false;

            for(List<Integer> badChunkCoordinate : badChunks)
                if(badChunkCoordinate.equals(chunkCoordinate)) {
                    registeredBadChunk = true;
                    break;
                }

            if(registeredBadChunk || isChunkFlooded(normalWorld.getChunkAt(chunkX, chunkY))) {
                badChunks.add(new ArrayList<>(chunkCoordinate));

                chunkX = random.nextInt(-1000, 1000);
                chunkY = random.nextInt(-1000, 1000);
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

        assert normalChunk != null;

        for(int x = 0; x < 16; x++)
            for(int z = 0; z < 16; z++)
                for(int y = getMiniGameWorld().getMinHeight(); y < getMiniGameWorld().getMaxHeight(); y++) {
                    Material sourceMaterial = normalChunk.getBlock(x, y, z).getType();
                    if(!sourceMaterial.isAir())
                        getCenterChunk().getBlock(x, y, z).setType(sourceMaterial);
                }

        for(int i = 0; i < 4; i++)
            spawnEntityInNaturalHabitat(EntityType.COW);

        for(int i = 0; i < 3; i++)
            spawnEntityInNaturalHabitat(EntityType.SHEEP);

        for(int i = 0; i < 2; i++)
            spawnEntityInNaturalHabitat(EntityType.CHICKEN);
    }

    @Override
    public void handleStart() {
        new RegionBinder(getCenterTopLocation(), getPlayersAndSpectators(), 64, 120);

        scoreboard = createGameScoreboard();

        seekersTeam = scoreboard.registerNewTeam("seekers");
        hidersTeam = scoreboard.registerNewTeam("hiders");

        seekersTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.FOR_OTHER_TEAMS);
        seekersTeam.setCanSeeFriendlyInvisibles(false);
        seekersTeam.setAllowFriendlyFire(false);
        seekersTeam.setPrefix(WorldEaterTeams.SEEKER.getFormattedDisplayName());
        seekersTeam.setColor(ChatColor.RED);

        hidersTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.FOR_OTHER_TEAMS);
        hidersTeam.setCanSeeFriendlyInvisibles(false);
        hidersTeam.setAllowFriendlyFire(false);
        hidersTeam.setPrefix(WorldEaterTeams.HIDER.getFormattedDisplayName());
        hidersTeam.setColor(ChatColor.GREEN);

        WorldEaterTeamSelectionMenu teamSelectionMenu = new WorldEaterTeamSelectionMenu(this, 40, menu -> {
            teamSet.putManyPlayersOnTeam(menu.getHiders(), WorldEaterTeams.HIDER);
            teamSet.putManyPlayersOnTeam(menu.getSeekers(), WorldEaterTeams.SEEKER);

            if(teamSet.isTeamEmpty(WorldEaterTeams.HIDER)) {
                sendGameMessage(new Message("No one wanted to play as a hider! Picking a random hider."));
                teamSet.putPlayerOnTeam(getRandomPlayer(), WorldEaterTeams.HIDER);
            } else if(teamSet.isTeamEmpty(WorldEaterTeams.SEEKER)) {
                sendGameMessage(new Message("No one wanted to play as a seeker! Picking a random seeker."));
                teamSet.putPlayerOnTeam(getRandomPlayer(), WorldEaterTeams.SEEKER);
            }

            gamePlay();
        });

        forEachPlayer(player -> {
            player.teleport(getSpawnLocation());
            PlayerState.prepareIdle(player, true);
            player.setGameMode(GameMode.SPECTATOR);

            teamSelectionMenu.openInventory(player);

            player.setScoreboard(scoreboard);
        });

        getFreezer().bulkFreeze(getPlayers());
    }

    @Override
    public void handlePlayerJoin(Player player) {

    }

    @Override
    public void handlePlayerLeave(Player player) {
        if(!isPlaying())
            return;

        if(teamSet.isTeamEmpty(WorldEaterTeams.HIDER)) { // Last hider left.
            sendGameMessage(new Message("§cThere is no hider remaining, so the game is over."));
            finish(GameResult.TIE);
        } else if(teamSet.isTeamEmpty(WorldEaterTeams.SEEKER)) { // Only hiders remain.
            sendGameMessage(new Message("§cThere is no seeker remaining, so the game is over."));
            finish(GameResult.TIE);
        } else if(getPlayers().size() == 1) { // Only 1 player remain.
            sendGameMessage(new Message("§cEverybody else quit. The game is over. :("));
            finish(GameResult.TIE);
        }
    }

    @Override
    public void handlePlayerRemoved(Player player) {
        getFreezer().unfreeze(player);

        teamSet.refreshPlayers();
    }

    @Override
    public int handleSoftStop() {
        playGameSound(Sound.BLOCK_BELL_USE, 3, 3);

        String header, subHeader;

        switch(result) {
            case SEEKERS_WIN:
                header = "§c§l" + TextBeautifier.beautify("Seekers", false);
                subHeader = "§7won the game";
                break;
            case HIDERS_WIN:
                header = "§a§l" + TextBeautifier.beautify("Hiders", false);
                subHeader = "§7won the game";
                break;
            default:
                header = "§e§l" + TextBeautifier.beautify("Tie", false);
                subHeader = "§7Nobody won the game.";
        }

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

        forEachSpectator(spectator -> spectator.sendTitle(
                header,
                subHeader,
                0, 20 * 6, 0
        ));

        sendGameMessage(new Message(header + " " + subHeader));

        Random random = new Random();
        for(int i = 0; i < 10; i++) {
            FireworkEffect effect = FireworkEffect.builder().flicker(false).trail(false).with(FireworkEffect.Type.STAR).withColor(Color.fromRGB(
                    random.nextInt(256),
                    random.nextInt(256),
                    random.nextInt(256)
            )).build();

            Location fireworkLocation = getCenterTopLocation().add(
                    random.nextInt(-16, 16),
                    0,
                    random.nextInt(-16, 16)
            );

            fireworkLocation.setY(getMiniGameWorld().getHighestBlockYAt(fireworkLocation) + random.nextInt(2, 10));

            Firework firework = getMiniGameWorld().spawn(fireworkLocation, Firework.class);

            FireworkMeta fireworkMeta = firework.getFireworkMeta();
            fireworkMeta.clearEffects();
            fireworkMeta.addEffect(effect);

            firework.setFireworkMeta(fireworkMeta);

            addTask(new BukkitRunnable() {
                public void run() {
                    firework.detonate();
                }
            }.runTaskLater(NoxetServer.getPlugin(), 20L * random.nextInt(1, 6)));
        }

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
        return teamSet.isPlayerOnTeam(player, WorldEaterTeams.HIDER) ? DeathContract.SPECTATE : DeathContract.RESPAWN_SAME_LOCATION_KEEP_INVENTORY;
    }

    @Override
    public List<ItemStack> handlePlayerDrops(Player deadPlayer) {
        if(teamSet.isPlayerOnTeam(deadPlayer, WorldEaterTeams.SEEKER)) {
            ItemStack item = ItemGenerator.generatePlayerSkull(
                    deadPlayer,
                    "§c§lGift of the Ghosts §8[ §eRight-click for invisibility §8]",
                    Collections.singletonList("§eRight-click to become invisible for 20 seconds.")
            );

            bindActionToItem(item, affectedPlayer -> {
                if(teamSet.isPlayerOnTeam(affectedPlayer, WorldEaterTeams.HIDER)) {
                    affectedPlayer.playSound(affectedPlayer, Sound.ENTITY_CAT_HISS, 1, 0.5f);
                    affectedPlayer.sendTitle("§aInvisible", "§3You are now §ninvisible§3.", 10, 20 * 3, 10);
                    new Message("§eYou are now invisible for §c20§e seconds.").send(affectedPlayer);

                    affectedPlayer.setInvisible(true);
                    addTask(new BukkitRunnable() {
                        @Override
                        public void run() {
                            affectedPlayer.setInvisible(false);
                            affectedPlayer.sendTitle("§c§lWAH!", "§eYou are no longer invisible.", 10, 20 * 3, 10);
                            new Message("§eYou are visible again.").send(affectedPlayer);
                        }
                    }.runTaskLater(NoxetServer.getPlugin(), 20 * 20));
                } else
                    new ErrorMessage(ErrorMessage.ErrorType.COMMON, "Only hiders can use this item!").send(affectedPlayer);

                affectedPlayer.getInventory().remove(item);
            });

            return Collections.singletonList(item);
        }

        return null;
    }

    @Override
    public void handleRespawn(Player player) {
        if(teamSet.isPlayerOnTeam(player, WorldEaterTeams.HIDER))
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
    private final List<WorldEaterEvents.GameEvent> events = new ArrayList<>();

    public WorldEater() {
        super(GameDefinition.WORLD_EATER);
    }

    private void gamePlay() {
        sendGameMessage(new Message("§aHiders are..."));

        forEachPlayer(player -> PlayerState.prepareIdle(player, true));

        teamSet.forEach(WorldEaterTeams.SEEKER, seeker -> seeker.sendTitle("§c§lSEEKER", "§eFind and eliminate the hiders.", 0, 20 * 5, 0));
        teamSet.forEach(WorldEaterTeams.HIDER, hider -> {
            hider.sendTitle("§a§lHIDER", "§eEndure the seekers attempts to kill you.", 0, 20 * 5, 0);
            sendGameMessage(new Message(" - §b" + hider.getName()));
        });

        sendGameMessage(new Message("§eGet ready! Game starts in §c5§e seconds..."));

        addTask(new BukkitRunnable() {
            @Override
            public void run() {
                getFreezer().empty();

                sendGameMessage(new Message("§eHiders are given a head start."));

                final ArrayList<BukkitTask> seekerCircleTasks = new ArrayList<>();

                teamSet.forEach(WorldEaterTeams.SEEKER, seeker -> {
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

                teamSet.forEach(WorldEaterTeams.HIDER, hider -> {
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
                            String timeLeftString = FancyTimeConverter.deltaSecondsToFancyTime(finalI, true);

                            if(finalI % 2 == 0)
                                playGameSound(Sound.BLOCK_POINTED_DRIPSTONE_FALL, 0.6f, 2);

                            if(finalI % 10 == 0)
                                sendGameMessage(new Message("§eSeekers are released in " + timeLeftString + "."));

                            teamSet.forEach(WorldEaterTeams.SEEKER, seeker -> seeker.sendTitle(timeLeftString, "§euntil released...", 0, 20 * 2, 0));
                            teamSet.forEach(WorldEaterTeams.HIDER, hider -> new ActionBarMessage("§8§l[ §" + (finalI % 2 == 0 ? "4" : "f") + "§l! §8§l] §bReleasing seekers in " + timeLeftString).send(hider));
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

                        setPvpRule(true);

                        playGameSound(Sound.BLOCK_ANVIL_LAND, 2, 2);

                        teamSet.forEach(WorldEaterTeams.SEEKER, seeker -> {
                            preparePlayer(seeker);
                            seeker.resetTitle();
                        });

                        teamSet.forEach(WorldEaterTeams.HIDER, hider -> new ActionBarMessage("§c§lSEEKERS RELEASED!").send(hider));

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
                                startY = Math.max(startY, getMiniGameWorld().getHighestBlockYAt(x, z));

                        long progress = 0;

                        for(int y = startY; y > -50; y--) {
                            int finalY = y;
                            progress += 20L * 30 * Math.pow(0.983d, 1 + startY - y);
                            addTask(new BukkitRunnable() {
                                @Override
                                public void run() {
                                    playGameSound(Sound.BLOCK_BAMBOO_WOOD_PRESSURE_PLATE_CLICK_OFF, 1, 1);

                                    for(int x = 0; x < 16; x++)
                                        for(int z = 0; z < 16; z++)
                                            getMiniGameWorld().setBlockData(getCenterChunk().getBlock(x, finalY, z).getLocation(), Material.AIR.createBlockData());
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
        return (teamSet.isPlayerOnTeam(player, WorldEaterTeams.SEEKER) && result == GameResult.SEEKERS_WIN) || (teamSet.isPlayerOnTeam(player, WorldEaterTeams.HIDER) && result == GameResult.HIDERS_WIN);
    }

    private void finish(GameResult result) {
        this.result = result;
        softStop();
    }

    protected Location getCenterTopLocation() {
        Location center = getCenterChunk().getBlock(8, 0, 8).getLocation();
        center.setY(getMiniGameWorld().getHighestBlockYAt(center));
        return center;
    }

    protected Location getAppropriateSpawnLocation(Location spawn) {
        int i = 0;

        while(i++ < 100)
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
        Random random = new Random();

        Location spawn = getCenterChunk().getBlock(
                random.nextInt(3, 14),
                0,
                random.nextInt(3, 14)
        ).getLocation();
        spawn.setY(getMiniGameWorld().getHighestBlockYAt(spawn));

        getMiniGameWorld().spawnEntity(getAppropriateSpawnLocation(spawn), type);
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
                "§c" + teamSet.countTeamPlayers(WorldEaterTeams.SEEKER) + "§e seeking",
                "§a" + teamSet.countTeamPlayers(WorldEaterTeams.HIDER) + "§e hiding",
                "§7---",
                "§7" + getSpectators().size() + "§8 spectating",
                "§7---",
                events.size() == 0 ? "§7No event" : events.get(0).eventName
        });
    }

    private static String getFancyTimeLeft(int countdown) {
        return (countdown >= 60 ? "§c" + (countdown / 60) + "§em " : "") + "§c" + countdown % 60 + "§es";
    }

    public void removeEvent(WorldEaterEvents.GameEvent event) {
        events.remove(event);
    }

    public TeamSet getTeamSet() {
        return teamSet;
    }
}
