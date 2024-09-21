package org.noxet.noxetserver.minigames.worldeater

import org.bukkit.*
import org.bukkit.block.{Biome, Block}
import org.bukkit.block.data.BlockData
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.*
import org.bukkit.event.EventHandler
import org.bukkit.event.block.{Action, BlockPlaceEvent}
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.inventory.{FurnaceSmeltEvent, FurnaceStartSmeltEvent}
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.generator.{BiomeProvider, WorldInfo}
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.FireworkMeta
import org.bukkit.potion.{PotionEffect, PotionEffectType}
import org.bukkit.scheduler.BukkitTask
import org.noxet.noxetserver.{Events, NoxetServer}
import org.noxet.noxetserver.menus.ItemGenerator
import org.noxet.noxetserver.menus.inventory.TeamPickerMenu
import org.noxet.noxetserver.messaging.ErrorMessage.ErrorType.Common
import org.noxet.noxetserver.messaging.{ActionBarMessage, ErrorMessage, Message}
import org.noxet.noxetserver.minigames.{GameDefinition, MiniGameController}
import org.noxet.noxetserver.minigames.worldeater.WorldEater.GameResult.*
import org.noxet.noxetserver.minigames.worldeater.WorldEater.badChunksConfig
import org.noxet.noxetserver.playerstate.PlayerState
import org.noxet.noxetserver.util.*

import java.io.{File, IOException}
import scala.annotation.tailrec

object WorldEater:
  enum GameResult:
    case SEEKERS_WIN, TIE, HIDERS_WIN

  private val cacheWorldName = "WORLDEATER_CACHE"

  @tailrec
  protected def getAppropriateSpawnLocation(spawn: Location, attempts: Int): Option[Location] =
    if spawn.getBlock.getType.isAir ||
      spawn.getBlock.getType.toString.contains("LEAVES") ||
      spawn.getBlock.getType.toString.contains("MUSHROOM")
    then
      getAppropriateSpawnLocation(spawn.subtract(0, 1, 0), attempts - 1) // Go down 1 block if air or leaf block.
    else if spawn.getBlock.getType.toString.endsWith("_LOG") then
      getAppropriateSpawnLocation(spawn.clone.add(1, 0, 0), attempts - 1) // Go x++ if wood log block.
    else Some(spawn.clone.add(0, 2, 0))

  private def hasLiquidOnTop(world: World, x: Int, z: Int): Boolean =
    (world.getMaxHeight to world.getMinHeight by -1)
      .view
      .map(y => world.getBlockAt(x, y, z))
      .find(_.getType.isAir)
      .exists(_.isLiquid)

  private def isChunkFlooded(chunk: Chunk): Boolean =
    (0 until 16).flatMap(x => (0 until 16).map(y => (x, y)))
      .count(
        hasLiquidOnTop(
          chunk.getWorld,
          (chunk.getX << 4) + x,
          (chunk.getZ << 4) + z)
      ) > 10

  private val lootDrops: List[Material] = List(
    Material.DIAMOND_AXE,
    Material.DIAMOND_PICKAXE,
    Material.DIAMOND_BLOCK,
    Material.DIAMOND_HELMET,
    Material.DIAMOND_CHESTPLATE,
    Material.DIAMOND_LEGGINGS,
    Material.NETHERITE_HELMET,
    Material.NETHERITE_BOOTS,
    Material.NETHERITE_SWORD,
    Material.TNT,
    Material.FLINT_AND_STEEL,
    Material.SPECTRAL_ARROW,
    Material.OAK_LOG,
    Material.DARK_OAK_LOG,
    Material.SPRUCE_LOG
  )

  private def getRandomLootItem: ItemStack =
    ItemStack(lootDrops.sampleOne)

  private def getRandomlyStackedLootItem(using random: Random): ItemStack =
    val itemStack = getRandomLootItem

    if itemStack.getType.getMaxStackSize > 1 then
      itemStack.setAmount(random.nextInt(3) + 1)

    itemStack

  private def badChunksFile: File =
    File(NoxetServer.getPlugin.getPluginDirectory,
      "world-eater-bad-chunks.yml")

  private def badChunksConfig: YamlConfiguration =
    YamlConfiguration.loadConfiguration(badChunksFile)

  private def fetchBadChunks: List[List[Integer]] =
    Option(badChunksConfig.getList("chunks").asInstanceOf[List[List[Integer]]])
      .getOrElse(List())

  private def updateBadChunks(badChunks: List[List[Integer]]): Unit =
    val config = badChunksConfig
    config.set("chunks", badChunks)
    config.save(badChunksFile)

  @tailrec
  private def findGoodChunk(isChunkGood: Chunk => Boolean,
                            world: World,
                            attempts: Int,
                            badChunks: List[Chunk])
                           (using random: Random): (Option[Chunk], List[Chunk]) =
    if attempts == 0 then
      return (None, badChunks)

    val chunk = world.getChunkAt(
      random.nextInt(2000) - 1000,
      random.nextInt(2000) - 1000
    )

    if isChunkGood(chunk) then
      (Some(chunk), badChunks)
    else
      findGoodChunk(isChunkGood, world, attempts - 1, badChunks + chunk)

  private def normalWorld: World =
    val normalWorldCreator = WorldCreator(cacheWorldName)
    normalWorldCreator.`type`(WorldType.NORMAL)
    normalWorldCreator.generateStructures(true)
    normalWorldCreator.biomeProvider(new BiomeProvider:
      override def getBiome(worldInfo: WorldInfo, i: Int, i1: Int, i2: Int): Biome =
        Biome.DARK_FOREST

      override def getBiomes(worldInfo: WorldInfo): List[Biome] =
        List(Biome.DARK_FOREST)
    )
    Option(normalWorldCreator.createWorld).get


class WorldEater extends MiniGameController(GameDefinition.WORLD_EATER):
  private val teamSet = TeamSet(getPlayers, WorldEaterTeams.SEEKER, WorldEaterTeams.HIDER)
  private val result = GameResult.TIE
  private val startY
  private val nextLayer
  private val layerRemoveSpeed
  private val nextLayerDisappearsAt
  private val fallingBlockLootBoxes = HashSet[FallingBlock]()
  private val placedLootBoxes = HashSet[Block]()
  private val playerPlacedBlocks = HashSet[Location]()
  private val seekerDeathCount = HashMap[Player, Integer]()

  given Random()

  override def handlePreStart(): Unit =
    val normalWorld = WorldEater.normalWorld

    setPvpRule(false)

    // Clone chunk

    val oldBadChunks = WorldEater.fetchBadChunks

    val (goodChunk, newBadChunks) = WorldEater.findGoodChunk(
      c => !oldBadChunks.contains(List(chunk.getX, chunk.getY)) && !isChunkFlooded(c),
      normalWorld,
      150,
      Nil
    )

    updateBadChunks(oldBadChunks ::: newBadChunks)

    goodChunk match
      case Some(source) =>
        val target = getCenterChunk

        for
          x <- 0 until 16
          z <- 0 until 16
          y <- getMiniGameWorld.getMinHeight until getMiniGameWorld.getMaxHeight
          sourceMaterial = source.getBlock(x, y, z).getType
          if !sourceMaterial.isAir
        do target.getBlock(x, y, z).setType(sourceMaterial)

        spawnEntities()
      case None =>
        sendGameMessage(Message(
          "Too many attempts! Gave up trying to find a good chunk."))
        stop()

  def spawnEntities(): Unit =
    Map(
      5 -> EntityType.PIG,
      4 -> EntityType.COW,
      3 -> EntityType.SHEEP,
      2 -> EntityType.CHICKEN,
    ).foreach(
      (amount, entity) => (0 until amount)
        .foreach(spawnEntityInNaturalHabitat(entity)))

  override def handleStart(): Unit =
    RegionBinder(getCenterTopLocation, getPlayersAndSpectators, 5 / 2 * 16, 120)

    val teamSelectionMenu = TeamPickerMenu(this, getTeamSet.getTeams, 40, menu =>
      teamSet.assignPlayersByTeamPickerMenu(menu)

      if teamSet.isTeamEmpty(WorldEaterTeams.HIDER) then
        sendGameMessage(Message("No one wanted to play as a hider! Picking a random hider."))
        teamSet.putPlayerOnTeam(getRandomPlayer, WorldEaterTeams.HIDER)
      else if teamSet.isTeamEmpty(WorldEaterTeams.SEEKER) then
        sendGameMessage(Message("No one wanted to play as a seeker! Picking a random seeker."))
        teamSet.putPlayerOnTeam(getRandomPlayer, WorldEaterTeams.SEEKER)

      phaseTeamsPicked()
    )

    forEachPlayer(player =>
      player.teleport(getSpawnLocation)
      PlayerState.prepareIdle(player, true)
      player.setGameMode(GameMode.SPECTATOR)

      teamSelectionMenu.openInventory(player)
    )

    getFreezer.bulkFreeze(getPlayers)

  override def handlePlayerJoin(player: Player): Unit = ()

  override def handlePlayerLeave(player: Player): Unit =
    if !isPlaying then return

    if teamSet.isTeamEmpty(WorldEaterTeams.HIDER) then // Last hider left.
      sendGameMessage(Message("§cThere is no hider remaining, so the game is over."))
      finish(GameResult.SEEKERS_WIN)
    else if teamSet.isTeamEmpty(WorldEaterTeams.SEEKER) then // Only hiders remain.
      sendGameMessage(Message("§cThere is no seeker remaining, so the game is over."))
      finish(GameResult.HIDERS_WIN)
    else if getPlayers.size == 1 then // Only 1 player remain.
      sendGameMessage(Message("§cEverybody else quit. The game is over. :("))
      finish(GameResult.TIE)

  override def handlePlayerRemoved(player: Player): Unit =
    getFreezer.unfreeze(player)
    teamSet.refreshPlayers()

  override def handleSoftStop: Int =
    playGameSound(Sound.BLOCK_BELL_USE, 3, 3)

    val (header, subHeader) = result match
      case SEEKERS_WIN =>
        (
          "§c§l" + TextBeautifier.beautify("Seekers", false),
          "§7won the game"
        )
      case HIDERS_WIN =>
        (
          "§a§l" + TextBeautifier.beautify("Hiders", false),
          "§7won the game"
        )
      case TIE =>
        (
          "§e§l" + TextBeautifier.beautify("Tie", false),
          "§7Nobody won the game"
        )

    forEachPlayer(player =>
      player.sendTitle(
        if didPlayerWin(player, result) then
          "§a§l" + TextBeautifier.beautify("Victory", false) + "!"
        else if result != GameResult.TIE then
          "§c§l" + TextBeautifier.beautify("Lost", false) + "!"
        else
          "§e§l" + TextBeautifier.beautify("Tie", false) + "!",

        if result == GameResult.SEEKERS_WIN then "§eSeekers won."
        else if result == GameResult.HIDERS_WIN then "§eHiders won."
        else "§7Nobody won.",
        0, 20 * 6, 0
      ))

    forEachSpectator(_.sendTitle(
      header,
      subHeader,
      0, 20 * 6, 0
    ))

    sendGameMessage(Message(header + " " + subHeader))

    val random = Random()
    for i <- 0 until 10 do
      val effect = FireworkEffect
        .builder
        .flicker(false)
        .trail(false)
        .`with`(FireworkEffect.Type.STAR)
        .withColor(Color.fromRGB(
          random.nextInt(256),
          random.nextInt(256),
          random.nextInt(256)
        )).build

      val fireworkLocation = getCenterTopLocation.add(
        random.nextInt(-16, 16),
        0,
        random.nextInt(-16, 16)
      )

      fireworkLocation.setY(
        getMiniGameWorld.getHighestBlockYAt(fireworkLocation) +
          random.nextInt(2, 10))

      val firework = getMiniGameWorld.spawn(fireworkLocation, Firework.`type`)

      val fireworkMeta = firework.getFireworkMeta
      fireworkMeta.clearEffects()
      fireworkMeta.addEffect(effect)

      firework.setFireworkMeta(fireworkMeta)

      scheduleTask(firework.detonate(), 20 * random.nextInt(1, 6))

    20 * 15

  override def handlePostStop(): Unit = teamSet.unregister()

  override def handleDeath(player: Player): DeathContract =
    if teamSet.isPlayerOnTeam(player, WorldEaterTeams.HIDER) then
      if teamSet.countTeamPlayers(WorldEaterTeams.HIDER) == 1 then // This was the last hider.
        finish(GameResult.SEEKERS_WIN)
      return DeathContract.SPECTATE

    DeathContract.RESPAWN_SAME_LOCATION_KEEP_INVENTORY

  override def handlePlayerDrops(deadPlayer: Player): List[ItemStack] =
    if teamSet.isPlayerOnTeam(deadPlayer, WorldEaterTeams.SEEKER) then
      val item = ItemGenerator.generatePlayerSkull(
        deadPlayer,
        "§c§lGift of the Ghosts §8[ §eRight-click for invisibility §8]",
        List("§eRight-click to become invisible for 20 seconds.")
      )

      bindActionToItem(item, affectedPlayer =>
        if teamSet.isPlayerOnTeam(affectedPlayer, WorldEaterTeams.HIDER) then
          if affectedPlayer.isInvisible then
            Message("§cYou are already invisible.").send(affectedPlayer)
            return

          affectedPlayer.playSound(affectedPlayer, Sound.ENTITY_CAT_HISS, 1, 0.5)
          affectedPlayer.sendTitle("§aInvisible", "§3You are now §ninvisible§3.", 10, 20 * 3, 10)
          Message("§eYou are now invisible for §c20§e seconds.").send(affectedPlayer)

          affectedPlayer.setInvisible(true)
          scheduleTask(() =>
            affectedPlayer.setInvisible(false)
            affectedPlayer.sendTitle(
              "§c§lWAH!",
              "§eYou are no longer invisible.",
              fadeIn: 10,
              stay: 20 * 3,
              fadeOut: 10)
            Message("§eYou are visible again.").send(affectedPlayer)
            , 20 * 20)
        else
          ErrorMessage(Common, "Only hiders can use this item!").send(affectedPlayer)

        affectedPlayer.getInventory.remove(item)
      )

      return List(item)

    null

  override def handleRespawn(player: Player): Unit =
    if teamSet.isPlayerOnTeam(player, WorldEaterTeams.HIDER) then return

    if player.getLocation.getY < getMiniGameWorld.getMinHeight then
      player.teleport(getSpawnLocation)

    getFreezer.freeze(player)
    player.setGameMode(GameMode.SPECTATOR)

    player.addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS, 10, 10, true, false))

    val newDeathCount = seekerDeathCount.getOrDefault(player, 0) + 1

    seekerDeathCount.put(player, newDeathCount)

    val secondsLeft = AtomicInteger(5 + newDeathCount - 1)

    scheduleTaskTimer(t =>
      if secondsLeft.get == 0 then
        player.setGameMode(GameMode.SURVIVAL)
        getFreezer.unfreeze(player)

        PlayerState.prepareNormal(player, false)

        player.teleport(getSpawnLocation)

        Events.setTemporaryInvulnerability(player)

        if newDeathCount == 5 then
          player.setHealthScale(0.5)
          player.setWalkSpeed(0.7)
          Message("§cDue to excessive deaths, your max health has been " +
            "limited, and your speed has been decreased.").send(player)
          player.sendTitle(
            "§4Excessive deaths",
            "§cMax health and walk speed decreased...",
            fadeIn: 10,
            stay: 20 * 4,
            fadeOut: 10)

        return

      player.sendTitle(
        "§c" + FancyTimeConverter.deltaSecondsToFancyTime(secondsLeft.getAndDecrement),
        "§euntil you respawn...",
        fadeIn: 0,
        stay: 20 * 2,
        fadeOut: 0)
      , 0, 20)

  override def getSpawnLocation: Location =
    getAppropriateSpawnLocation(getCenterTopLocation)

  private val timeLeft
  private val preparedEvents = HashMap[Integer, WorldEaterEvents.GameEvent]()
  private val currentEvents = List[WorldEaterEvents.GameEvent]()

  private def phaseTeamsPicked(): Unit =
    forEachPlayer(PlayerState.prepareIdle(_, true))

    teamSet.forEach(
      WorldEaterTeams.SEEKER,
      _.sendTitle("§c§lSEEKER", "§eFind and eliminate the hiders.", 0, 20 * 5, 0)
    )

    sendGameMessage(Message("§eThe §ahiders§e are..."))

    teamSet.forEach(
      WorldEaterTeams.HIDER,
      hider =>
        hider.sendTitle("§a§lHIDER", "§eEndure the seekers attempts to kill you.", 0, 20 * 5, 0)
        sendGameMessage(Message(s" - §b${hider.getName}"))
    )

    sendGameMessage(Message("§eThe rest are §cseekers§e."))

    sendGameMessage(Message("§eGet ready! Game starts in §c5§e seconds..."))

    scheduleTask(phaseHeadStart(), 20 * 5)


  private def phaseHeadStart(): Unit =
    getFreezer.empty()

    // Prepare the chunk muncher (do it already here to let the hiders place blocks before seekers spawn):

    startY = 0

    for
      x <- 0 until 16
      z <- 0 until 16
    do
      startY = Math.max(
        startY,
        getMiniGameWorld.getHighestBlockYAt(
          getCenterChunk.getBlock(x, 0, z).getLocation
        )
      )

    nextLayer = startY

    sendGameMessage(Message("§eHiders are given a head start."))

    val seekerCircleTasks = List[BukkitTask]()

    teamSet.forEach(WorldEaterTeams.SEEKER, seeker =>
      PlayerState.prepareIdle(seeker, true)

      val center = getCenterTopLocation.add(0, 20, 0)

      val speed = 0.1

      val angle = List(0)

      seekerCircleTasks.add(scheduleTaskTimer(() =>
        if !seeker.isFlying then
          seeker.setFlying(true)

        val radians = Math.toRadians(angle(0))

        center.setYaw(
          Math.toDegrees(
            Math.atan2(
              -Math.cos(radians),
              Math.sin(radians))
          ).toFloat)
        center.setPitch(90)
        seeker.teleport(center)

        angle(0) += speed
        angle(0) %= 360
        , 0, 2))
    )

    teamSet.forEach(WorldEaterTeams.HIDER, hider =>
      preparePlayer(hider)

      hider.playSound(hider, Sound.BLOCK_NOTE_BLOCK_BASS, 1, 0.5f)
      hider.sendTitle("§c§lHURRY UP!", "§ePrepare and reach §3shelter§e fast!", 5, 20 * 5, 10)
    )

    val secondsToRelease = teamSet.countTeamPlayers(WorldEaterTeams.SEEKER) * 60

    for i <- secondsToRelease until 0 by -1 do
      scheduleTask(() =>
        val timeLeftString = FancyTimeConverter.deltaSecondsToFancyTime(i, true)

        if i % 2 == 0 then
          playGameSound(Sound.BLOCK_POINTED_DRIPSTONE_FALL, 0.6f, 2)

        if i % 10 == 0 then
          sendGameMessage(Message(s"§eSeekers are released in $timeLeftString."))

        teamSet.forEach(
          WorldEaterTeams.SEEKER,
          _.sendTitle(timeLeftString, "§euntil released...", 0, 20 * 2, 0))
        teamSet.forEach(
          WorldEaterTeams.HIDER,
          ActionBarMessage(
            s"§8§l[ §${if i % 2 == 0 then "4" else "f"}§l! §8§l] §bReleasing seekers in $timeLeftString"
          ).send(hider))
        , 20 * (secondsToRelease - i))

    scheduleTask(phaseSeekersReleased(seekerCircleTasks), 20 * secondsToRelease)

  private def phaseSeekersReleased(seekerCircleTasks: Seq[BukkitTask]): Unit =
    seekerCircleTasks.foreach(_.cancel())

    getFreezer.empty()

    sendGameMessage(Message("§c§lSEEKERS HAVE BEEN RELEASED!"))

    setPvpRule(true)

    playGameSound(Sound.BLOCK_ANVIL_LAND, 2, 2)

    teamSet.forEach(
      WorldEaterTeams.SEEKER,
      seeker =>
        preparePlayer(seeker)
        seeker.resetTitle()
    )

    teamSet.forEach(
      WorldEaterTeams.HIDER,
      ActionBarMessage("§c§lSEEKERS RELEASED!").send
    )

    spawnEntities()

    sendGameMessage(Message(
      "§cThe Chunk Muncher is eating up the island! " +
        "See more info in the stats to the right."))

    removeNextLayer()

    sendGameMessage(Message(
      "§eIf the hiders survive until the game is " +
        "over, they win. Otherwise the seekers win."))

    timeLeft = 30 * 60

    prepareEvent(WorldEaterEvents.GameEvent.HOT_SUN, 27)

    prepareEvent(WorldEaterEvents.GameEvent.QUICK_STOVE, 25)

    prepareEvent(WorldEaterEvents.GameEvent.VISIBLE_HIDERS, 22)
    prepareEvent(WorldEaterEvents.GameEvent.VISIBLE_HIDERS, 8)

    prepareEvent(WorldEaterEvents.GameEvent.METEOR_RAIN, 20)

    prepareEvent(WorldEaterEvents.GameEvent.LOOT_DROP, 15)

    prepareEvent(WorldEaterEvents.GameEvent.DRILLING, 12)

    prepareEvent(WorldEaterEvents.GameEvent.EXPLODING_HORSES, 5)

    prepareEvent(WorldEaterEvents.GameEvent.EVERYONE_VISIBLE, 1)

    scheduleTaskTimer(task =>
      teamSet.updateScoreboard(
        "§c" + FancyTimeConverter.deltaSecondsToFancyTime(timeLeft, true) + "§e remaining",
        "§7---",
        s"§4\uD83D\uDDE1§c Seeking: §e${teamSet.countTeamPlayers(WorldEaterTeams.SEEKER)}",
        s"§2\uD83C\uDF56§a Hiding: §e${teamSet.countTeamPlayers(WorldEaterTeams.HIDER)}",
        "§7---",
        s"§8☠§7 Spectating: §e${getSpectators.size}",
        "§7---",
        "§3§nChunk Muncher",
        s"§3 - Y-level §b$nextLayer",
        s"§3 - Velocity §b${layerRemoveSpeed / 20}s/layer",
        //"§3 - Next layer §b" + FancyTimeConverter.deltaSecondsToFancyTime((int) (nextLayerDisappearsAt - System.currentTimeMillis()) / 1000),
        "§7---",
        if currentEvents.isEmpty then
          "§7No current event"
        else
          s"§9§lEVENT: §b${currentEvents.get(0).getEventName}"
      )

      if timeLeft % 60 == 0 then
        val minutesRemaining = timeLeft / 60

        if minutesRemaining != 0 && (minutesRemaining % 5 == 0 || minutesRemaining < 10) then
          sendGameMessage(Message(s"§eThe game has §c$minutesRemaining§e minutes remaining."))

        dispatchEvents(minutesRemaining)

      if timeLeft == 0 then
        sendGameMessage(Message("§aTime has gone out! Hiders win."))
        finish(GameResult.HIDERS_WIN)
        task.cancel() // Prevent time from getting negative.

      timeLeft -= 1
      , 40, 20)

  private def prepareEvent(event: WorldEaterEvents.GameEvent, minutesRemaining: Int): Unit =
    preparedEvents.put(minutesRemaining, event)

  private def dispatchEvents(minutesRemaining: Int): Unit =
    val eventHere = preparedEvents.get(minutesRemaining)

    if eventHere == null then
      return

    currentEvents.add(eventHere)
    eventHere.getEventConsumer.accept(this, Promise(() =>
      currentEvents.remove(eventHere)
      sendGameMessage(Message(s"§f${eventHere.getEventName}§c event is now over."))
      , 20 * 60 * 10))


  private def removeNextLayer(): Unit =
    if nextLayer < getMiniGameWorld.getMinHeight + 10 then return

    removeLayer(nextLayer)
    nextLayer -= 1

    layerRemoveSpeed = (20 * 30 * Math.pow(0.983d, 1 + startY - nextLayer)).toInt
    nextLayerDisappearsAt = System.currentTimeMillis + layerRemoveSpeed * 50 // ticks * 50 = ms

    scheduleTask(removeNextLayer(), layerRemoveSpeed)

  private def removeLayer(noMarginY: Int): Unit =
    playGameSound(Sound.BLOCK_BAMBOO_WOOD_PRESSURE_PLATE_CLICK_OFF, 1, 1)

    for placedLootBox <- placedLootBoxes do
      if placedLootBox.getLocation.getY >= noMarginY then
        placedLootBoxes.remove(placedLootBox)

        val newBoxLocation = placedLootBox.getLocation.clone
        newBoxLocation.setY(noMarginY)

        val blockData = placedLootBox.getBlockData

        scheduleTask(() =>
          val newBox = getMiniGameWorld.getBlockAt(newBoxLocation)
          newBox.setBlockData(blockData)

          placedLootBoxes.add(newBox)
          , 21)

    val random = Random()

    for chunk <- getAllocatedChunks do
      for
        x <- 0 until 16
        z <- 0 until 16
        // Clear 3 Y-levels of blocks. This is the block-placing margin.
        // Without this, players would be unable to place blocks on top of chunk at all.
        y <- noMarginY until noMarginY + 3
        block = chunk.getBlock(x, y, z)
        if block.getType != Material.AIR
      do
        scheduleTask(() =>
          if !playerPlacedBlocks.remove(block.getLocation) || !block.breakNaturally then
            getMiniGameWorld.setBlockData(block.getLocation, Material.AIR.createBlockData)
            getMiniGameWorld.spawnParticle(Particle.SWEEP_ATTACK, block.getLocation, 1)
          , if !block.isLiquid then random.nextInt(20) else 0)

  def didPlayerWin(player: Player, result: GameResult): Boolean =
    (teamSet.isPlayerOnTeam(player, WorldEaterTeams.SEEKER) &&
      result == GameResult.SEEKERS_WIN) ||
      (teamSet.isPlayerOnTeam(player, WorldEaterTeams.HIDER) &&
        result == GameResult.HIDERS_WIN)

  private def finish(result: GameResult): Unit =
    this.result = result
    softStop()

  protected def getCenterTopLocation: Location =
    val (x, z) = (8, 8)
    getCenterChunk.getBlock(
      x,
      getMiniGameWorld.getHighestBlockYAt(x, z),
      z
    ).getLocation

  private def spawnEntityInNaturalHabitat(kind: EntityType): Unit =
    val random = Random()

    val (x, z) = (random.nextInt(3, 14), random.nextInt(3, 14))

    val spawn = getCenterChunk.getBlock(
      x,
      getMiniGameWorld.getHighestBlockYAt(x, z),
      z
    ).getLocation

    getMiniGameWorld.spawnEntity(getAppropriateSpawnLocation(spawn), kind)

  def getTeamSet: TeamSet = teamSet

  @EventHandler def onBlockPlace(e: BlockPlaceEvent): Unit =
    if !isPlayer(e.getPlayer) then return

    if e.getBlock.getLocation.getY > nextLayer + 2 then
      ErrorMessage(Common,
        "§o** Your hand got bit by the storm of the Chunk Muncher, as you reached to place a block. **").send(e.getPlayer)
      e.setCancelled(true)
      e.getPlayer.damage(2)
    else
      playerPlacedBlocks.add(e.getBlock.getLocation)

  @EventHandler def onEntityChangeBlock(e: EntityChangeBlockEvent): Unit =
    if e.getEntity.getType == EntityType.FALLING_BLOCK &&
      fallingBlockLootBoxes.remove(e.getEntity.asInstanceOf[FallingBlock]) then
      placedLootBoxes.add(e.getBlock)
      sendGameMessage(Message("§9A loot box has dropped! Claim it for rewards."))

  @EventHandler def onPlayerInteract(e: PlayerInteractEvent): Unit =
    if (e.getAction == Action.LEFT_CLICK_BLOCK ||
      e.getAction == Action.RIGHT_CLICK_BLOCK) &&
      placedLootBoxes.remove(e.getClickedBlock)
    then
      e.getClickedBlock.setType(Material.AIR)

      val lootDropLocation = e.getClickedBlock.getLocation

      (0 until 6).foreach(
        getMiniGameWorld.dropItemNaturally(lootDropLocation, getRandomlyStackedLootItem))

      sendGameMessage(Message(s"§cThe loot box has been looted by ${e.getPlayer.getName}."))

  @EventHandler def onFurnaceStartSmelt(e: FurnaceStartSmeltEvent): Unit =
    if doesOwnLocation(e.getBlock.getLocation) &&
      currentEvents.contains(WorldEaterEvents.GameEvent.QUICK_STOVE)
    then
      e.setTotalCookTime(e.getTotalCookTime / 5)

  @EventHandler def onFurnaceSmelt(e: FurnaceSmeltEvent): Unit =
    if doesOwnLocation(e.getBlock.getLocation) && currentEvents.contains(WorldEaterEvents.GameEvent.QUICK_STOVE) then
      val result = e.getResult
      result.setAmount(2)
      e.setResult(result)