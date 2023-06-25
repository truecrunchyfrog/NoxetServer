package org.noxet.noxetserver.minigames;

import org.apache.commons.io.FileUtils;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Consumer;
import org.noxet.noxetserver.NoxetServer;
import org.noxet.noxetserver.RealmManager;
import org.noxet.noxetserver.messaging.ErrorMessage;
import org.noxet.noxetserver.messaging.Message;
import org.noxet.noxetserver.messaging.MessagingContext;
import org.noxet.noxetserver.messaging.channels.MessagingGameChannel;
import org.noxet.noxetserver.playerstate.PlayerState;
import org.noxet.noxetserver.util.ConcatSet;
import org.noxet.noxetserver.util.PlayerFreezer;
import org.noxet.noxetserver.util.TextBeautifier;

import java.io.IOException;
import java.util.*;

public abstract class MiniGameController implements Listener {
    public static final String gameWorldPrefix = "NOXET_GAME_WORLD_";

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
    private World workingWorld;
    private final MiniGameOptions options;
    private final PlayerFreezer freezer;

    private final MessagingContext messagingContext;

    private BukkitTask startTask;

    private final Map<ItemStack, Consumer<Player>> actionBoundItems = new HashMap<>();

    /**
     * Any delayed BukkitTask related to this game should be added to tasks with the addTask() method, to make sure that they are canceled on stop.
     */
    private final List<BukkitTask> tasks = new ArrayList<>();

    public MiniGameController(GameDefinition game) {
        gameId = String.valueOf(new Random().nextInt((int) Math.pow(10, 5), (int) Math.pow(10, 6)));
        this.game = game;
        options = game.getOptions();
        state = MiniGameState.STALLING;

        workingWorld = null;

        messagingContext = new MessagingContext("§3" + TextBeautifier.beautify(options.getDisplayName(), false) + " ", new MessagingGameChannel(this));

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

        state = MiniGameState.PLAYING;

        WorldCreator worldCreator = new WorldCreator(gameWorldPrefix + gameId);

        if(options.getWorldCreator() != null)
            worldCreator.copy(options.getWorldCreator());

        workingWorld = NoxetServer.getPlugin().getServer().createWorld(worldCreator);

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

        startTask = new BukkitRunnable() {
            @Override
            public void run() {
                attemptStart();
            }
        }.runTaskLater(NoxetServer.getPlugin(), ticksBeforeAttempt);

        addTask(startTask);
    }

    private void attemptStart() {
        if(!enoughPlayers()) {
            sendGameMessage(new Message("§cNot enough players to start."));
            stop();
            return;
        }

        sendGameMessage(new Message("§bFinally! The game is starting..."));

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
            removeSpectator(player);

        players.add(player);

        if(hasStarted())
            preparePlayer(player);

        sendGameMessage(new Message("§b" + player.getName() + "§3 joined the game. §7(§e" + players.size() + "§7/§e" + options.getMaxPlayers() + "§7)"));

        handlePlayerJoin(player);

        touchInit();

        return true;
    }

    public void removePlayer(Player player) {
        if(!isPlayer(player))
            return;

        players.remove(player);
        handlePlayerLeave(player);
        disconnectPlayerFromGame(player);

        touchInit();

        sendGameMessage(new Message("§c" + player.getName() + "§4 left the game."));

        if(players.size() == 0)
            stop();
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
            removePlayer(player);

        spectators.add(player);

        if(hasStarted())
            prepareSpectator(player);

        sendGameMessage(new Message("§e" + player.getName() + "§7 is now spectating the game."));

        return true;
    }

    public void removeSpectator(Player player) {
        if(!isSpectator(player))
            return;

        spectators.remove(player);
        disconnectPlayerFromGame(player);

        sendGameMessage(new Message("§e" + player.getName() + "§7 is no longer spectating the game."));
    }

    public void disconnectPlayerFromGame(Player player) {
        handlePlayerRemoved(player);
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
        player.teleport(workingWorld.getSpawnLocation());
        PlayerState.prepareDefault(player);
        player.setGameMode(options.getDefaultGameMode());
    }

    public void prepareSpectator(Player player) {
        player.teleport(workingWorld.getSpawnLocation());
        PlayerState.prepareDefault(player);
        player.setGameMode(GameMode.SPECTATOR);
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
        int ticks = Math.min(handleSoftStop(), 20 * 60);

        new BukkitRunnable() {
            @Override
            public void run() {
                stop(); // Tired of waiting: hard stop the game now.
            }
        }.runTaskLater(NoxetServer.getPlugin(), ticks); // Wait at most 60 seconds.

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

        MiniGameManager.unregisterGame(this);

        HandlerList.unregisterAll(this); // Stop listening for events.

        if(workingWorld != null) {
            for(Player player : workingWorld.getPlayers()) // Kick all players from the world.
                disconnectPlayerFromGame(player);

            NoxetServer.getPlugin().getServer().unloadWorld(workingWorld, false); // Unload the game world.

            try {
                FileUtils.deleteDirectory(workingWorld.getWorldFolder());
            } catch(IOException e) {
                throw new RuntimeException(e);
            }
        }

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
    public void onEntityPortal(EntityPortalEvent e) {
        // Prevent entities' usage of portals.
        if(e.getFrom().getWorld() == workingWorld)
            e.setCancelled(true);
    }

    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent e) {
        // Prevent players' usage of portals.
        if(e.getFrom().getWorld() == workingWorld)
            e.setCancelled(true);
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
                case RESPAWN_KEEP_INVENTORY:
                    e.setKeepInventory(true);
                    e.setKeepLevel(true);
                    e.getDrops().clear();
                    e.setDroppedExp(0);
                case RESPAWN_SAME_LOCATION_KEEP_INVENTORY:
                    Location oldSpawnLocation = e.getEntity().getBedSpawnLocation();

                    Location deathLocation = e.getEntity().getLastDeathLocation();
                    if(deathLocation != null && deathLocation.getY() > workingWorld.getMinHeight())
                        e.getEntity().setBedSpawnLocation(deathLocation, true);

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            e.getEntity().setBedSpawnLocation(oldSpawnLocation, true);
                        }
                    }.runTaskLater(NoxetServer.getPlugin(), 1);
                case RESPAWN_DROP_INVENTORY:
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            e.getEntity().spigot().respawn();
                        }
                    }.runTaskLater(NoxetServer.getPlugin(), 0);
                    break;
                case SPECTATE:
                    if(!addSpectator(e.getEntity()))
                        removePlayer(e.getEntity()); // If player cannot spectate, just remove them from the game.
                    break;
            }

            List<ItemStack> drops = handlePlayerDrops(e.getEntity());
            if(drops != null)
                e.getDrops().addAll(drops);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent e) {
        if(isPlayer(e.getPlayer()))
            handleRespawn(e.getPlayer());
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent e) {
        if(e.getFrom() == workingWorld)
            disbandPlayer(e.getPlayer());
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean canPlayerModifyWorld(Player player) {
        return hasStarted() && !freezer.isPlayerFrozen(player);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        if(e.getBlock().getWorld() == workingWorld && !canPlayerModifyWorld(e.getPlayer()))
            e.setCancelled(true);
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent e) {
        if(e.getPlayer().getWorld() == workingWorld && !canPlayerModifyWorld(e.getPlayer()))
            e.setCancelled(true);
    }

    @EventHandler
    public void onEntityPickupItem(EntityPickupItemEvent e) {
        if(e.getItem().getWorld() == workingWorld && e.getEntity() instanceof Player && !canPlayerModifyWorld((Player) e.getEntity()))
            e.setCancelled(true);
    }

    @EventHandler
    public void onEntityHurtEntity(EntityDamageByEntityEvent e) {
        if(e.getDamager().getWorld() == workingWorld && e.getDamager() instanceof Player && !canPlayerModifyWorld((Player) e.getDamager()))
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

    public World getWorkingWorld() {
        return workingWorld;
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
}
