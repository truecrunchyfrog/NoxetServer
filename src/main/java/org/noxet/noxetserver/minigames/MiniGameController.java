package org.noxet.noxetserver.minigames;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Consumer;
import org.noxet.noxetserver.NoxetServer;
import org.noxet.noxetserver.messaging.ActionBarMessage;
import org.noxet.noxetserver.messaging.ErrorMessage;
import org.noxet.noxetserver.messaging.Message;
import org.noxet.noxetserver.messaging.MessagingContext;
import org.noxet.noxetserver.messaging.channels.MessagingGameChannel;
import org.noxet.noxetserver.minigames.party.Party;
import org.noxet.noxetserver.playerstate.PlayerState;
import org.noxet.noxetserver.realm.RealmManager;
import org.noxet.noxetserver.util.ConcatSet;
import org.noxet.noxetserver.util.PlayerFreezer;
import org.noxet.noxetserver.util.TextBeautifier;

import java.util.*;

public abstract class MiniGameController implements Listener {
    public enum MiniGameState {
        STALLING, PLAYING, ENDED
    }

    public enum DeathContract {
        RESPAWN_DROP_INVENTORY, RESPAWN_KEEP_INVENTORY, RESPAWN_SAME_LOCATION_KEEP_INVENTORY, SPECTATE
    }

    private final String gameId;
    private final GameDefinition game;
    private MiniGameState state;
    private final Set<Player> players = new HashSet<>(),
            spectators = new HashSet<>();
    private final ConcatSet<Player> allPlayers = new ConcatSet<>(players, spectators);
    private final MiniGameOptions options;
    private final PlayerFreezer freezer;

    private final MessagingContext messagingContext;

    private BukkitTask startTask;

    private final Map<ItemStack, Consumer<Player>> actionBoundItems = new HashMap<>();

    private boolean pvpAllowed = true;
    private long startTimestamp = 0;

    private final List<Chunk> allocatedChunks = new ArrayList<>();

    /**
     * Any delayed BukkitTask related to this game should be added to tasks with the addTask() method, to make sure that they are canceled on stop.
     */
    private final List<BukkitTask> tasks = new ArrayList<>();

    public MiniGameController(GameDefinition game) {
        gameId = String.valueOf(new Random().nextInt((int) Math.pow(10, 5), (int) Math.pow(10, 6)));
        this.game = game;
        options = game.getOptions();
        state = MiniGameState.STALLING;

        messagingContext = new MessagingContext("§3§l" + TextBeautifier.beautify(options.getDisplayName()) + "§7 :: ", new MessagingGameChannel(this));

        NoxetServer.getPlugin().getServer().getPluginManager().registerEvents(this, NoxetServer.getPlugin());

        MiniGameManager.registerGame(this);

        freezer = new PlayerFreezer(1);
    }

    public String getGameId() {
        return gameId;
    }

    public GameDefinition getGame() {
        return game;
    }

    public MiniGameState getState() {
        return state;
    }

    public void start() {
        if(hasStarted())
            return;

        if(startTask != null)
            startTask.cancel();

        state = MiniGameState.PLAYING;

        startTimestamp = System.currentTimeMillis();

        allocateChunks();

        handlePreStart();

        for(Player player : players)
            preparePlayer(player);

        for(Player spectator : spectators)
            prepareSpectator(spectator);

        handleStart();
    }

    /**
     * Called before the players are warped to the world (is only run when the player starts, not when players drop-in after start).
     * In this method, the game world should be prepared so that a teleportation is appropriate.
     */
    public abstract void handlePreStart();

    /**
     * Called when the game has otherwise initialized. Players are warped. The world should already have been mostly set up.
     */
    public abstract void handleStart();

    /**
     * Called when a player has joined the game.
     * @param player The player that joined the game
     */
    public abstract void handlePlayerJoin(Player player);

    /**
     * Called when a player leaves the game.
     * @param player The player that left the game
     */
    public abstract void handlePlayerLeave(Player player);

    /**
     * Called when a player or spectator has been removed from the game.
     * Used to clean up player from variables and such.
     * @param player The player that was removed from the game
     */
    public abstract void handlePlayerRemoved(Player player);

    /**
     * Called when the game is over, but still running.
     * @return The ticks to wait before stopping the game
     */
    public abstract int handleSoftStop();

    /**
     * Called when the game has stopped. Use to clean up necessary things, such as objectives, teams, etc.
     * Things that should always happen upon stop, even during hard stops, should be placed here.
     */
    public abstract void handlePostStop();

    /**
     * Called when a player dies.
     * @param player The player that died
     * @return What should happen with the player
     */
    public abstract DeathContract handleDeath(Player player);

    /**
     * Called when a player's death item drops are selected.
     * @implNote These drops will only append to the drops, and not replace them (for example, if the player does not have keep inventory, then their items will drop together with this)
     * @param player The player who died
     * @return The drops that will spawn where the player died
     */
    public abstract List<ItemStack> handlePlayerDrops(Player player);

    /**
     * Called when a player respawns.
     * @param player The player that respawned
     */
    public abstract void handleRespawn(Player player);

    /**
     * Get the default spawn location for the game.
     * This is where the players will be warped automatically upon start.
     * @return The game's spawn location
     */
    public abstract Location getSpawnLocation();


    private void touchInit() {
        if(hasStarted())
            return;

        if(startTask != null)
            startTask.cancel();

        int ticksBeforeAttempt;

        if(!enoughPlayers()) { // Too few players.
            ticksBeforeAttempt = 20 * 30;
            sendGameMessage(new Message("§eNeed §7§n" + (options.getMinPlayers() - players.size()) + "§e more to start."));
        } else if(!isFull()) { // Enough players, but more can join.
            ticksBeforeAttempt = 20 * 20;
            sendGameMessage(new Message("§eEnough players gathered!"));
        } else { // Game is full.
            ticksBeforeAttempt = 20 * 5;
            sendGameMessage(new Message("§eGame is filled up!"));
        }

        sendGameMessage(new Message("§aPreliminary start in §e" + ticksBeforeAttempt / 20 + "s" + "§a..."));
        sendGameMessage(new ActionBarMessage("§eYour game is starting soon!"));

        startTask = new BukkitRunnable() {
            @Override
            public void run() {
                attemptStart();
            }
        }.runTaskLater(NoxetServer.getPlugin(), ticksBeforeAttempt);

        addTask(startTask);
    }

    public void attemptStart() {
        if(!enoughPlayers()) {
            sendGameMessage(new Message("§cNot enough players to start."));
            stop();
            return;
        }

        sendGameMessage(new Message("§bFinally! The game is starting..."));
        sendGameMessage(new ActionBarMessage("§3The game is starting!"));

        start();
    }


    /**
     * Adds a player to the game. Can be used on spectators during game, if drop-in is allowed.
     * @param player The player to add to the game
     * @return True if player was added, false if it failed
     */
    public boolean addPlayer(Player player) {
        if(isPlayer(player)) {
            new ErrorMessage(ErrorMessage.ErrorType.COMMON, "You are already in this game.").send(player);
            return false;
        }

        if(isPlaying() && !options.allowPlayerDropIns()) {
            new ErrorMessage(ErrorMessage.ErrorType.COMMON, "This game is already running.").send(player);
            return false;
        }

        if(hasEnded()) {
            new ErrorMessage(ErrorMessage.ErrorType.COMMON, "This game has ended.").send(player);
            return false;
        }

        if(isFull()) {
            new ErrorMessage(ErrorMessage.ErrorType.COMMON, "This game is full.").send(player);
            return false;
        }

        if(isSpectator(player))
            removeSpectator(player, false);

        players.add(player);

        if(hasStarted())
            preparePlayer(player);

        sendGameMessage(new Message("§b" + player.getName() + "§3 joined the game. §7(§e" + players.size() + "§7/§e" + options.getMaxPlayers() + "§7)"));

        handlePlayerJoin(player);

        touchInit();

        return true;
    }

    public void removePlayer(Player player) {
        removePlayer(player, true);
    }

    public void removePlayer(Player player, boolean disconnect) {
        if(!isPlayer(player))
            return;

        players.remove(player);

        handlePlayerLeave(player);

        if(disconnect)
            disconnectPlayerFromGame(player);

        if(players.size() > 0) {
            if(!hasEnded()) {
                touchInit();
                sendGameMessage(new Message("§c" + player.getName() + "§4 left the game."));
            }
        } else if(!disconnect)
            new BukkitRunnable() {
                @Override
                public void run() {
                    softStop(); // Delay to allow players to become spectators, in that case.
                }
            }.runTaskLater(NoxetServer.getPlugin(), 0);
        else
            stop();
    }

    public void addParty(Party party) {
        party.sendPartyMessage(new Message("§aThe party has collectively entered a game."));

        for(Player member : party.getMembers())
            addPlayer(member);
    }

    public boolean addSpectator(Player player) {
        if(options.getSpectatorContract() != MiniGameOptions.SpectatorContract.ALL && !isPlayer(player)) {
            new ErrorMessage(ErrorMessage.ErrorType.COMMON, "This game does not allow outside spectators.").send(player);
            return false;
        }

        if(isFullForSpectators()) {
            new ErrorMessage(ErrorMessage.ErrorType.COMMON, "Enough spectators are in this game.").send(player);
            return false;
        }

        if(isPlayer(player))
            removePlayer(player, false);

        spectators.add(player);

        if(hasStarted())
            prepareSpectator(player);

        sendGameMessage(new Message("§e" + player.getName() + "§7 is now spectating the game."));

        return true;
    }

    public void removeSpectator(Player player) {
        removeSpectator(player, true);
    }

    public void removeSpectator(Player player, boolean disconnect) {
        if(!isSpectator(player))
            return;

        spectators.remove(player);

        if(disconnect)
            disconnectPlayerFromGame(player);

        sendGameMessage(new Message("§e" + player.getName() + "§7 is no longer spectating the game."));
    }

    public void disconnectPlayerFromGame(Player player) {
        handlePlayerRemoved(player);
        if(hasStarted())
            RealmManager.goToHub(player);
    }

    public boolean isPlayer(Player player) {
        return players.contains(player);
    }

    public boolean isSpectator(Player player) {
        return spectators.contains(player);
    }

    public Set<Player> getPlayers() {
        return players;
    }

    public Set<Player> getSpectators() {
        return spectators;
    }

    public Set<Player> getPlayersAndSpectators() {
        return allPlayers;
    }

    public boolean isFull() {
        return players.size() == options.getMaxPlayers();
    }

    public boolean isFullForSpectators() {
        return spectators.size() >= 100;
    }

    /**
     * Check if the enough players are in the game to start it.
     * @return true if enough players are in-game, otherwise false
     */
    public boolean enoughPlayers() {
        return players.size() >= options.getMinPlayers();
    }

    public void preparePlayer(Player player) {
        player.teleport(getSpawnLocation());
        PlayerState.prepareDefault(player);
        player.setGameMode(options.getDefaultGameMode());

        assignPlayerTime(player);
    }

    public void prepareSpectator(Player player) {
        player.teleport(getSpawnLocation());
        PlayerState.prepareDefault(player);
        player.setGameMode(GameMode.SPECTATOR);

        assignPlayerTime(player);
    }

    public void assignPlayerTime(Player player) {
        player.setPlayerTime(getTicksSinceStart() - getMiniGameWorld().getTime(), true);
    }

    /**
     * Whether the game is in play state.
     * @return True if the game is in play state
     */
    public boolean isPlaying() {
        return state == MiniGameState.PLAYING;
    }

    /**
     * Whether the game has started (either is playing, or has ended).
     * @return True if the game has started
     */
    public boolean hasStarted() {
        return state != MiniGameState.STALLING;
    }

    /**
     * Whether the game has ended.
     * @return True if the game has ended
     */
    public boolean hasEnded() {
        return state == MiniGameState.ENDED;
    }

    /**
     * Stops the game properly. Game-specific events are called. After events are finished the game will run stop().
     * @return The ticks to wait before the game is stopped
     */
    public int softStop() {
        state = MiniGameState.ENDED;

        int ticks = Math.min(handleSoftStop(), 20 * 60);

        sendGameMessage(
                new Message("§a§lTHE GAME IS OVER!\n")
                        .addButton("Play again", ChatColor.GREEN, "Join another queue for this game", "game play " + game.getOptions().getId())
                        .addButton("Different game", ChatColor.DARK_AQUA, "Find another game to play", "games")
                        .addButton("Lobby", ChatColor.RED, "Head back to hub", "game leave")
        );

        addTask(new BukkitRunnable() {
            @Override
            public void run() {
                stop(); // Tired of waiting: hard stop the game now.
            }
        }.runTaskLater(NoxetServer.getPlugin(), ticks)); // Wait at most 60 seconds.

        return ticks;
    }

    /**
     * Stops the game immediately. Unregisters this game instance, cancels tasks, unregisters event listener, removes players from world, unloads world, and deletes world.
     */
    public void stop() {
        state = MiniGameState.ENDED;

        freezer.empty();

        for(BukkitTask task : tasks)
            task.cancel();

        for(Player player : getPlayersAndSpectators()) // Kick all players from the world.
            disconnectPlayerFromGame(player);

        MiniGameManager.unregisterGame(this);

        HandlerList.unregisterAll(this); // Stop listening for events.

        handlePostStop();
    }

    /**
     * Disbands a player from this game. Attempts to remove as both player and spectator.
     * @param player The player/spectator to remove from the game
     */
    public void disbandPlayer(Player player) {
        removePlayer(player);
        removeSpectator(player);
    }

    @EventHandler
    public void onBedEnterEvent(PlayerBedEnterEvent e) {
        if(isPlayer(e.getPlayer()))
            e.setCancelled(true);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        disbandPlayer(e.getPlayer());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        if(isPlayer(e.getEntity())) {
            DeathContract deathContract = handleDeath(e.getEntity());

            switch(deathContract) {
                case RESPAWN_SAME_LOCATION_KEEP_INVENTORY:
                    Location oldSpawnLocation = e.getEntity().getBedSpawnLocation();

                    Location deathLocation = e.getEntity().getLastDeathLocation();
                    if(deathLocation != null && deathLocation.getY() > getMiniGameWorld().getMinHeight())
                        e.getEntity().setBedSpawnLocation(deathLocation, true);

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            e.getEntity().setBedSpawnLocation(oldSpawnLocation, true);
                        }
                    }.runTaskLater(NoxetServer.getPlugin(), 2);
                case RESPAWN_KEEP_INVENTORY:
                    e.setKeepInventory(true);
                    e.setKeepLevel(true);
                    e.getDrops().clear();
                    e.setDroppedExp(0);
                case RESPAWN_DROP_INVENTORY:
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            e.getEntity().spigot().respawn();
                        }
                    }.runTaskLater(NoxetServer.getPlugin(), 0);
                    break;
                case SPECTATE:
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if(addSpectator(e.getEntity())) {
                                new Message("You died. Now spectating.").send(e.getEntity());
                                e.getEntity().spigot().respawn();
                            } else {
                                removePlayer(e.getEntity()); // If player cannot spectate, just remove them from the game.
                                new Message("§cSorry. Could not spectate.").send(e.getEntity());
                            }
                        }
                    }.runTaskLater(NoxetServer.getPlugin(), 0);
                    break;
            }

            List<ItemStack> drops = handlePlayerDrops(e.getEntity());
            if(drops != null)
                e.getDrops().addAll(drops);
        } else if(isSpectator(e.getEntity()))
            prepareSpectator(e.getEntity());
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent e) {
        if(isPlayer(e.getPlayer()) && hasStarted()) {
            handleRespawn(e.getPlayer());
        } else if(!isSpectator(e.getPlayer()))
            return;

        e.setRespawnLocation(getSpawnLocation());
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent e) {
        if(isGameWorld(e.getFrom()))
            disbandPlayer(e.getPlayer());
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean canPlayerModifyWorld(Player player) {
        return hasStarted() && !freezer.isPlayerFrozen(player);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        if(isGameWorld(e.getBlock().getWorld()) && !canPlayerModifyWorld(e.getPlayer()))
            e.setCancelled(true);
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent e) {
        if(isGameWorld(e.getPlayer().getWorld()) && !canPlayerModifyWorld(e.getPlayer()))
            e.setCancelled(true);
    }

    @EventHandler
    public void onEntityPickupItem(EntityPickupItemEvent e) {
        if(isGameWorld(e.getItem().getWorld()) && e.getEntity() instanceof Player && !canPlayerModifyWorld((Player) e.getEntity()))
            e.setCancelled(true);
    }

    @EventHandler
    public void onEntityHurtEntity(EntityDamageByEntityEvent e) {
        if(
                isGameWorld(e.getDamager().getWorld()) &&
                e.getDamager() instanceof Player &&
                (!canPlayerModifyWorld((Player) e.getDamager()) || ( // Cancel if game does not allow world modification.
                        !pvpAllowed && e.getEntity() instanceof Player // Cancel if PVP is disabled.
                ))
        )
            e.setCancelled(true);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        if(isPlayer(e.getPlayer()) && (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) && actionBoundItems.containsKey(e.getItem())) {
            e.setCancelled(true);
            actionBoundItems.remove(e.getItem()).accept(e.getPlayer());
        }
    }

    public void sendGameMessage(Message message) {
        messagingContext.broadcast(message);
    }

    public void playGameSound(Sound sound, float volume, float pitch) {
        for(Player playerInGame : getPlayersAndSpectators())
            playerInGame.playSound(playerInGame, sound, volume, pitch);
    }

    public MiniGameOptions getOptions() {
        return options;
    }

    public static World getMiniGameWorld() {
        return new WorldCreator("mini_game_world").generator(new ChunkGenerator() {
            @Override
            @SuppressWarnings({"NullableProblems", "deprecation"})
            public ChunkData generateChunkData(World world, Random random, int x, int z, BiomeGrid biome) {
                return createChunkData(world);
            }
        }).createWorld();
    }

    public static boolean isGameWorld(World world) {
        return getMiniGameWorld().equals(world);
    }

    public void addTask(BukkitTask task) {
        tasks.add(task);
    }

    public PlayerFreezer getFreezer() {
        return freezer;
    }

    public Player getRandomPlayer() {
        return getRandomPlayer(false);
    }

    public Player getRandomPlayer(boolean notFrozen) {
        Set<Player> availablePlayers = new HashSet<>(players);

        if(notFrozen) {
            availablePlayers.removeAll(freezer.getFrozenPlayers());

            if(availablePlayers.size() == 0)
                return getRandomPlayer();
        }

        return availablePlayers.toArray(new Player[0])[(int) (availablePlayers.size() * Math.random())];
    }

    public void bindActionToItem(ItemStack itemStack, Consumer<Player> action) {
        actionBoundItems.put(itemStack, action);
    }

    public void setPvpRule(boolean enabled) {
        pvpAllowed = enabled;
    }

    public long getTicksSinceStart() {
        return (System.currentTimeMillis() - startTimestamp) / 20_000;
    }

    private void allocateChunks() {
        allocatedChunks.clear();

        int chunksSquared = options.getWorldChunksSquared();

        int worldChunksSquared = 60_000 / 16 - chunksSquared;

        Random random = new Random();

        int offsetX = random.nextInt(worldChunksSquared), offsetZ = random.nextInt(worldChunksSquared);

        for(int x = 0; x < chunksSquared; x++)
            for(int z = 0; z < chunksSquared; z++) {
                Chunk chunk = getMiniGameWorld().getChunkAt(x + offsetX, z + offsetZ);

                for(int bX = 0; bX < 16; bX++)
                    for(int bZ = 0; bZ < 16; bZ++)
                        for(int bY = getMiniGameWorld().getMinHeight(); bY < getMiniGameWorld().getMaxHeight(); bY++) {
                            Block block = chunk.getBlock(bX, bY, bZ);
                            if(!block.getType().isAir())
                                block.setBlockData(Material.AIR.createBlockData());
                        }

                for(Entity entity : chunk.getEntities())
                    entity.remove();

                allocatedChunks.add(chunk);
            }
    }

    public List<Chunk> getAllocatedChunks() {
        return allocatedChunks;
    }

    public Chunk getCenterChunk() {
        return allocatedChunks.get(allocatedChunks.size() / 2);
    }

    public void forEachPlayer(Consumer<Player> playerConsumer) {
        for(Player player : getPlayers())
            playerConsumer.accept(player);
    }

    public void forEachSpectator(Consumer<Player> spectatorConsumer) {
        for(Player player : getSpectators())
            spectatorConsumer.accept(player);
    }
}
