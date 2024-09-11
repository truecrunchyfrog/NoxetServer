package org.noxet.noxetserver.minigames

import net.md_5.bungee.api.ChatColor
import org.bukkit.*
import org.bukkit.block.Block
import org.bukkit.entity.{Entity, EntityType, Pig, Player}
import org.bukkit.event.block.{Action, BlockBreakEvent}
import org.bukkit.event.entity.{EntityDamageByEntityEvent, EntityPickupItemEvent, PlayerDeathEvent}
import org.bukkit.event.player.*
import org.bukkit.event.{EventHandler, HandlerList, Listener}
import org.bukkit.generator.ChunkGenerator
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.{BukkitRunnable, BukkitTask}
import org.bukkit.util.Consumer
import org.noxet.noxetserver.NoxetServer
import org.noxet.noxetserver.messaging.channels.MessagingGameChannel
import org.noxet.noxetserver.messaging.{ActionBarMessage, ErrorMessage, Message, MessagingContext}
import org.noxet.noxetserver.minigames.MiniGameController.getMiniGameWorld
import org.noxet.noxetserver.minigames.party.Party
import org.noxet.noxetserver.playerstate.PlayerState
import org.noxet.noxetserver.realm.RealmManager
import org.noxet.noxetserver.util.*

import java.util.Random
import scala.collection.mutable

abstract class MiniGameController(val game: GameDefinition) extends Listener:
    enum MiniGameState:
        case STALLING, PLAYING, ENDED

    enum DeathContract:
        case RESPAWN_DROP_INVENTORY, RESPAWN_KEEP_INVENTORY, RESPAWN_SAME_LOCATION_KEEP_INVENTORY, SPECTATE

    val gameId: String = Random().nextInt(Math.pow(10, 5).toInt, Math.pow(10, 6).toInt).toString
    val options: MiniGameOptions = game.getOptions
    private var state = MiniGameState.STALLING
    private val messagingContext = MessagingContext(s"§3§l${TextBeautifier.beautify(options.getDisplayName)}§7 :: ", MessagingGameChannel(this))
    val freezer = PlayerFreezer(1)
    private val players = mutable.HashSet[Player]()
    private val spectators = mutable.HashSet[Player]()
    private val allPlayers = ConcatSet(players, spectators)
    private val actionBoundItems = mutable.HashMap[ItemStack, Player => Unit]()
    private val allocatedChunks = mutable.List()
    /** Any delayed BukkitTask related to this game should be added to tasks with taskSet.push() method, to make sure that they are canceled on stop. */
    private val taskSet = ControllableTaskSet()

    private var startTask: Option[BukkitTask] = None
    private var pvpAllowed = true
    private var startTimestamp = 0

    NoxetServer.getPlugin.getServer.getPluginManager.registerEvents(this, NoxetServer.getPlugin)
    MiniGameManager.registerGame(this)

    def start(): Unit =
        if hasStarted then return

        startTask.foreach(_.cancel)
        startTask = None

        state = MiniGameState.PLAYING

        startTimestamp = System.currentTimeMillis

        allocateChunks()

        handlePreStart()

        players.foreach(preparePlayer)
        spectators.foreach(prepareSpectator)

        handleStart()

    /**
     * Called before the players are warped to the world (is only run when the player starts, not when players drop in after start).
     * In this method, the game world should be prepared so that a teleportation is appropriate.
     */
    def handlePreStart(): Unit

    /**
     * Called when the game has otherwise initialized. Players are warped. The world should already have been mostly set up.
     */
    def handleStart(): Unit

    /**
     * Called when a player has joined the game.
     * @param player The player that joined the game
     */
    def handlePlayerJoin(player: Player): Unit

    /**
     * Called after a player leaves the game.
     *
     * @param player The player that left the game
     */
    def handlePlayerLeave(player: Player): Unit

    /**
     * Called when a player or spectator has been removed from the game.
     * Used to clean up player from variables and such.
     *
     * @param player The player that was removed from the game
     */
    def handlePlayerRemoved(player: Player): Unit

    /**
     * Called when the game is over, but still running.
     *
     * @return The ticks to wait before stopping the game
     */
    def handleSoftStop: Int

    /**
     * Called when the game has stopped. Used to clean up necessary things, such as objectives, teams, etc.
     * Things that should always happen upon stop, even during hard stops, should be placed here.
     */
    def handlePostStop(): Unit

    /**
     * Called when a player dies.
     *
     * @param player The player that died
     * @return What should happen with the player
     */
    def handleDeath(player: Player): DeathContract

    /**
     * Called when a player's death item drops are selected.
     *
     * @note These drops will only append to the drops, and not replace them (for example, if the player does not have keep inventory, then their items will drop together with this)
     * @param player The player who died
     * @return The drops that will spawn where the player died
     */
    def handlePlayerDrops(player: Player): Seq[ItemStack]

    /**
     * Called when a player respawns.
     *
     * @param player The player that respawned
     */
    def handleRespawn(player: Player): Unit

    /**
     * Get the default spawn location for the game.
     * This is where the players will be warped automatically upon start.
     *
     * @return The game's spawn location
     */
    def getSpawnLocation: Location


    def touchInit(): Unit =
        if hasStarted then return

        startTask.foreach(_.cancel)

        val ticksBeforeAttempt =
            if !enoughPlayers then // Too few players.
                sendGameMessage(Message(s"§eNeed §7§n${options.getMinPlayers - players.size}§e more to start."))
                20 * 30
            else if !isFull then // Enough players, but more can join.
                sendGameMessage(Message("§eEnough players gathered!"))
                20 * 20
            else // Game is full.
                sendGameMessage(Message("§eGame is filled up!"))
                20 * 5

        sendGameMessage(Message(s"§aPreliminary start in §e${ticksBeforeAttempt / 20}s§a..."))

        val startTimestamp = System.currentTimeMillis + ticksBeforeAttempt / 20 * 1000

        startTask = scheduleTaskTimer(t => {
            if System.currentTimeMillis > startTimestamp then
                t.cancel()
                attemptStart()
                return

            val secondRemainder = (System.currentTimeMillis / 1000) % 7

            sendGameMessage(ActionBarMessage(
                if secondRemainder <= 4 then
                    s"§eStarting in §6${FancyTimeConverter.deltaSecondsToFancyTime((startTimestamp - System.currentTimeMillis).toInt / 1000 + 1)} §7| §3${players.size}§7 of " +
                      (if enoughPlayers then s"§3${options.getMaxPlayers}§7 players" else s"§3${options.getMinPlayers}§7 players required")
                else
                    "§cExit this queue with §n/game leave"
            ))
        }, 0, 20)

    def attemptStart(): Unit =
        if !enoughPlayers then
            sendGameMessage(Message("§cNot enough players to start."))
            sendGameMessage(ActionBarMessage("§cNot enough players!"))
            stop()
            return

        sendGameMessage(Message("§bFinally! The game is starting..."))
        sendGameMessage(ActionBarMessage("§3The game is starting!"))

        start()


    /**
     * Adds a player to the game. Can be used on spectators during game, if drop-in is allowed.
     *
     * @param player The player to add to the game
     * @return `true` if player was added, otherwise `false`
     */
    def addPlayer(player: Player): Boolean =
        if isPlayer(player) then
            ErrorMessage(ErrorMessage.ErrorType.COMMON, "You are already in this game.").send(player)
            return false

        if isPlaying && !options.allowPlayerDropIns then
            ErrorMessage(ErrorMessage.ErrorType.COMMON, "This game is already running.").send(player)
            return false

        if hasEnded then
            ErrorMessage(ErrorMessage.ErrorType.COMMON, "This game has ended.").send(player)
            return false

        if isFull then
            ErrorMessage(ErrorMessage.ErrorType.COMMON, "This game is full.").send(player)
            return false

        if isSpectator(player) then
            removeSpectator(player, false)

        players.add(player)

        if hasStarted then
            preparePlayer(player)

        sendGameMessage(Message(s"§b${player.getName}§3 joined the game. §7(§e${players.size}§7/§e${options.getMaxPlayers}§7)"))

        handlePlayerJoin(player)

        touchInit()

        true

    def removePlayer(player: Player, disconnect: boolean = true): Unit =
        if !isPlayer(player) then return

        players.remove(player)

        handlePlayerLeave(player)

        if disconnect then
            disconnectPlayerFromGame(player)

        if players.size > 0 then
            if !hasEnded then
                touchInit()
                sendGameMessage(Message(s"§c${player.getName}§4 left the game."))
        else if !disconnect then
            scheduleTask(softStop, 0) // Delay to allow players to become spectators, in that case.
        else
            stop()

    def addParty(party: Party): Unit =
        party.sendPartyMessage(Message("§aThe party has collectively entered a game."))
        party.getMembers.foreach(addPlayer)

    def addSpectator(player: Player): Boolean =
        if options.getSpectatorContract != MiniGameOptions.SpectatorContract.ALL && !isPlayer(player) then
            ErrorMessage(ErrorMessage.ErrorType.COMMON, "This game does not allow outside spectators.").send(player)
            return false

        if isFullForSpectators then
            ErrorMessage(ErrorMessage.ErrorType.COMMON, "Enough spectators are in this game.").send(player)
            return false

        if isPlayer(player) then
            removePlayer(player, false)

        spectators.add(player)

        if hasStarted then
            prepareSpectator(player)

        sendGameMessage(Message(s"§e${player.getName}§7 is now spectating the game."))

        true

    def removeSpectator(player: Player): Unit = removeSpectator(player, true)

    def removeSpectator(player: Player, disconnect: Boolean): Unit =
        if !isSpectator(player) then return

        spectators.remove(player)

        if disconnect then
            disconnectPlayerFromGame(player)

        sendGameMessage(Message(s"§e${player.getName}§7 is no longer spectating the game."))

    def disconnectPlayerFromGame(player: Player): Unit =
        handlePlayerRemoved(player)
        if hasStarted then
            RealmManager.goToHub(player)

    def isPlayer(player: Player): Boolean = players.contains(player)

    def isSpectator(player: Player): Boolean = spectators.contains(player)

    def getPlayers: Set[Player] = players

    def getSpectators: Set[Player] = spectators

    def getPlayersAndSpectators: Set[Player] = allPlayers

    def isFull: Boolean = players.size == options.getMaxPlayers

    def isFullForSpectators: Boolean = spectators.size >= 100

    /**
     * Check if the enough players are in the game to start it.
     *
     * @return `true` if enough players are in-game, otherwise `false`
     */
    def enoughPlayers: Boolean = players.size >= options.getMinPlayers

    def preparePlayer(player: Player): Unit =
        player.teleport(getSpawnLocation)
        PlayerState.prepareDefault(player)
        player.setGameMode(options.getDefaultGameMode)

        assignPlayerTime(player)

    def prepareSpectator(player: Player): Unit =
        player.teleport(getSpawnLocation())
        PlayerState.prepareDefault(player)
        player.setGameMode(GameMode.SPECTATOR)

        assignPlayerTime(player)

    def assignPlayerTime(player: Player): Unit =
        player.setPlayerTime(getTicksSinceStart - getMiniGameWorld.getTime, true)

    /**
     * Whether the game is in play state.
     *
     * @return true if the game is in play state
     */
    def isPlaying: Boolean = state == MiniGameState.PLAYING

    /**
     * Whether the game has started (either is playing, or has ended).
     *
     * @return true if the game has started
     */
    def hasStarted: Boolean = state != MiniGameState.STALLING

    /**
     * Whether the game has ended.
     *
     * @return true if the game has ended
     */
    def hasEnded: Boolean = state == MiniGameState.ENDED

    /**
     * Stops the game properly. Game-specific events are called. After events are finished the game will run stop().
     *
     * @return The ticks to wait before the game is stopped
     */
    def softStop: Int =
        state = MiniGameState.ENDED

        val ticks = Math.min(handleSoftStop, 20 * 60)

        sendGameMessage(
            Message("§a§lTHE GAME IS OVER!\n")
              .addButton("Play again", ChatColor.GREEN, "Join another queue for this game", "game play " + game.getOptions.getId)
                        .addButton("Different game", ChatColor.DARK_AQUA, "Find another game to play", "games")
                        .addButton("Lobby", ChatColor.RED, "Head back to hub", "game leave")
        )

        scheduleTask(stop, ticks) // Wait at most 60 seconds.

        ticks

    /**
     * Stops the game immediately. Unregisters this game instance, cancels tasks, unregisters event listener, removes players from world, unloads world, and deletes world.
     */
    def stop(): Unit =
        startTask.foreach(_.cancel)

        freezer.empty()

        taskSet.abortAll()

        getPlayersAndSpectators.foreach(disconnectPlayerFromGame) // Kick all players from the world.

        state = MiniGameState.ENDED

        MiniGameManager.unregisterGame(this)

        HandlerList.unregisterAll(this) // Stop listening for events.

        handlePostStop()

    /**
     * Disbands a player from this game. Attempts to remove as both player and spectator.
     *
     * @param player The player/spectator to remove from the game
     */
    def disbandPlayer(player: Player): Unit =
        removePlayer(player)
        removeSpectator(player)

    @EventHandler def onBedEnterEvent(e: PlayerBedEnterEvent): Unit =
        if isPlayer(e.getPlayer) then e.setCancelled(true)

    @EventHandler def onPlayerQuit(e: PlayerQuitEvent): Unit = disbandPlayer(e.getPlayer)

    @EventHandler def onPlayerDeath(e: PlayerDeathEvent): Unit =
        if isPlayer(e.getEntity) then
            val deathContract = handleDeath(e.getEntity)

            deathContract match
                case RESPAWN_DROP_INVENTORY =>
                    new BukkitRunnable:
                        @Override def run(): Unit = e.getEntity.spigot.respawn()
                      .runTaskLater(NoxetServer.getPlugin, 0)
                case RESPAWN_KEEP_INVENTORY =>
                    e.setKeepInventory(true)
                    e.setKeepLevel(true)
                    e.getDrops.clear()
                    e.setDroppedExp(0)
                case RESPAWN_SAME_LOCATION_KEEP_INVENTORY =>
                    val oldSpawnLocation = e.getEntity.getBedSpawnLocation

                    Option(e.getEntity.getLastDeathLocation) match
                        case Some(deathLocation) if deathLocation.getY > getMiniGameWorld.getMinHeight =>
                            e.getEntity.setBedSpawnLocation(deathLocation, true)
                        case _ => ()

                    new BukkitRunnable:
                        @Override def run(): Unit = e.getEntity.setBedSpawnLocation(oldSpawnLocation, true)
                      .runTaskLater(NoxetServer.getPlugin, 2)
                case SPECTATE =>
                    new BukkitRunnable:
                        @Override def run(): Unit =
                            if addSpectator(e.getEntity) then
                                Message("You died. Now spectating.").send(e.getEntity)
                                e.getEntity.spigot.respawn()
                            else
                                removePlayer(e.getEntity) // If player cannot spectate, just remove them from the game.
                                Message("§cSorry. Could not spectate.").send(e.getEntity)
                      .runTaskLater(NoxetServer.getPlugin, 0)

            e.getDrops.addAll(handlePlayerDrops(e.getEntity))
        else if isSpectator(e.getEntity) then
            prepareSpectator(e.getEntity)

    @EventHandler def onPlayerRespawn(e: PlayerRespawnEvent): Unit =
        if !isSpectator(e.getPlayer) then return

        if isPlayer(e.getPlayer) && hasStarted then
            handleRespawn(e.getPlayer)

        e.setRespawnLocation(getSpawnLocation)

    @EventHandler def onPlayerChangedWorld(e: PlayerChangedWorldEvent): Unit =
        if isGameWorld(e.getFrom) then
            disbandPlayer(e.getPlayer)

    private def canPlayerModifyWorld(player: Player): Boolean = hasStarted && !freezer.isPlayerFrozen(player)

    @EventHandler def onBlockBreak(e: BlockBreakEvent): Unit =
        if isGameWorld(e.getBlock.getWorld) && !canPlayerModifyWorld(e.getPlayer) then
            e.setCancelled(true)

    @EventHandler def onPlayerDropItem(e: PlayerDropItemEvent): Unit =
        if isGameWorld(e.getPlayer.getWorld) && !canPlayerModifyWorld(e.getPlayer) then
            e.setCancelled(true)

    @EventHandler def onEntityPickupItem(e: EntityPickupItemEvent): Unit =
        if isGameWorld(e.getItem.getWorld) then e.getEntity match
            case p: Player if !canPlayerModifyWorld(e.getEntity.asInstanceOf[Player]) => e.setCancelled(true)
            case _ => ()

    @EventHandler def onEntityHurtEntity(e: EntityDamageByEntityEvent): Unit =
        if isGameWorld(e.getDamager.getWorld) then e.getDamager match
            case p: Player
                if !canPlayerModifyWorld(p) ||
                  (!pvpAllowed && e.getEntityType == EntityType.PLAYER) =>
                e.setCancelled(true)
            case _ => ()

    @EventHandler def onPlayerInteract(e: PlayerInteractEvent): Unit =
        if isPlayer(e.getPlayer) &&
          (e.getAction == Action.RIGHT_CLICK_AIR || e.getAction == Action.RIGHT_CLICK_BLOCK) &&
          actionBoundItems.contains(e.getItem) then
            e.setCancelled(true)
            actionBoundItems.remove(e.getItem)(e.getPlayer)

    def sendGameMessage(message: Message): Unit = messagingContext.broadcast(message)

    def playGameSound(sound: Sound, volume: float, pitch: float): Unit =
        getPlayersAndSpectators.foreach(_.playSound(_, sound, volume, pitch))

    def scheduleTask(runnable: Runnable, delayTicks: int): Unit =
        taskSet.push(new BukkitRunnable:
            override def run(): Unit = runnable.run()
          .runTaskLater(NoxetServer.getPlugin, delayTicks))

    def scheduleTaskTimer(runnable: BukkitRunnable => Unit, delayTicks: Int, periodTicks: Int): BukkitTask =
        val task = new BukkitRunnable:
            override def run(): Unit = runnable(this)
          .runTaskTimer(NoxetServer.getPlugin, delayTicks, periodTicks)

        taskSet.push(task)
        task

    def scheduleTaskTimer(runnable: Runnable, delayTicks: Int, periodTicks: Int): BukkitTask =
        scheduleTaskTimer(runnable.run(), delayTicks, periodTicks)

    def getRandomPlayer(excludeFrozen: Boolean = false): Player =
        val availablePlayers = if !excludeFrozen then players else players.filterNot(isPlayerFrozen)
        availablePlayers(Random().nextInt(availablePlayers.size))

    def bindActionToItem(itemStack: ItemStack, action: Consumer[Player]): Unit = actionBoundItems.put(itemStack, action)

    def setPvpRule(enabled: boolean): Unit = pvpAllowed = enabled

    def getTicksSinceStart: Long = (System.currentTimeMillis - startTimestamp) / 20_000

    private def allocateChunks(): Unit =
        allocatedChunks.clear()

        val chunksSquared = options.getWorldChunksSquared

        val worldChunksSquared = 60_000 / 16 - chunksSquared

        val random = Random()

        val offsetX = random.nextInt(worldChunksSquared)
        val offsetZ = random.nextInt(worldChunksSquared)

        for
            x <- chunksSquared
            z <- chunksSquared
            chunk = getMiniGameWorld.getChunkAt(x + offsetX, z + offsetZ)
        do
            for
                bX <- 0 until 16
                bZ <- 0 until 16
                bY <- getMiniGameWorld.getMinHeight until getMiniGameWorld.getMaxHeight
                block = chunk.getWorld(bX, bY, bZ)
                if !block.getType().isAir()
            do block.setBlockData(Material.AIR.createBlockData)

            chunk.getEntities.foreach(_.remove)
            allocatedChunks.add(chunk)

    def getAllocatedChunks: List[Chunk] = allocatedChunks

    def doesOwnLocation(location: Location): Boolean = allocatedChunks.contains(location.getChunk)

    def getCenterChunk: Chunk = allocatedChunks.get(allocatedChunks.size / 2)

object MiniGameController:
    def getMiniGameWorld: World =
        WorldCreator("mini_game_world").generator(new ChunkGenerator:
            override def generateChunkData(world: World, random: Random, x: int, z: int, biome: BiomeGrid): ChunkData = createChunkData(world)
        ).createWorld()

    def isGameWorld(world: World): Boolean = getMiniGameWorld.equals(world)

