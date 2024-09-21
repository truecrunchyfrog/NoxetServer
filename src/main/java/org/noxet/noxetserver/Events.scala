package org.noxet.noxetserver

import org.bukkit.*
import org.bukkit.advancement.AdvancementDisplay
import org.bukkit.block.data
import org.bukkit.entity.Player
import org.bukkit.event.block.*
import org.bukkit.event.entity.*
import org.bukkit.event.entity.EntityDamageEvent.DamageCause
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.*
import org.bukkit.event.server.ServerListPingEvent
import org.bukkit.event.{EventHandler, EventPriority, Listener}
import org.bukkit.potion.{PotionEffect, PotionEffectType}
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.Vector
import org.noxet.noxetserver.commands.misc.ChickenLeg
import org.noxet.noxetserver.commands.social.MsgConversation
import org.noxet.noxetserver.commands.teleportation.TeleportAsk
import org.noxet.noxetserver.menus.inventory.{GameNavigationMenu, SettingsMenu, SocialMenu}
import org.noxet.noxetserver.menus.inventorysetups.HubInventorySetup
import org.noxet.noxetserver.messaging.*
import org.noxet.noxetserver.minigames.party.Party
import org.noxet.noxetserver.minigames.worldeater.WorldEatertype.Bed
import org.noxet.noxetserver.minigames.{MiniGameController, MiniGameManager}
import org.noxet.noxetserver.playerdata.PlayerDataManager
import org.noxet.noxetserver.realm.RealmManager
import org.noxet.noxetserver.realm.RealmManager.*
import org.noxet.noxetserver.util.*
import org.spigotmc.event.player.PlayerSpawnLocationEvent

object Events extends Listener:
  enum TemporaryCommand(command: String):
    case ReadBeforeChat extends TemporaryCommand("read-before-chat")
    case UnderstandChat extends TemporaryCommand("understand-chat")
    case ConfirmBedSpawn extends TemporaryCommand("confirm-bed-spawn")
    case UnderstandAnarchy extends TemporaryCommand("understand-anarchy")

    def isMessageThisCommand(e: PlayerCommandPreprocessEvent): Boolean =
      e.getMessage.equalsIgnoreCase(getSlashCommand)

    def getRawCommand: String = command

    def getSlashCommand: String = s"/$command"

  @EventHandler def onPlayerTeleport(e: PlayerTeleportEvent): Unit =
    if CombatLogging.isCombatLogged(e.getPlayer) then
      CombatLogging.triggerLocationDisband(e.getPlayer)
      Message("§cYou teleported away while combat logged and was killed in penalty.").send(e.getPlayer)

      QuickRunnable(() =>
        if !e.getPlayer.isOnline then return

        val fromRealm = getRealmFromWorld(e.getFrom.getWorld)
        val toRealm = getCurrentRealm(e.getPlayer)

        if fromRealm == toRealm then
          CombatLoggingStorageManager().combatLogRejoin(e.getPlayer, toRealm)
      ).runTaskLater(NoxetServer.getPlugin, 1)

    if e.getTo != null &&
      e.getFrom.getWorld != e.getTo.getWorld &&
      e.getTo.getWorld != null
    then // Teleporting to another world.
      if e.getTo.getWorld.getName.equals("world") then
        goToHub(e.getPlayer)
        e.setCancelled(true)
        return

      val toRealm = getRealmFromWorld(e.getTo.getWorld)

      migrateToRealm(e.getPlayer, toRealm) // Migrator will send the player to spawn or last location in realm.

  // Don't cancel the teleportation!

  @EventHandler def onPlayerChangedWorld(e: PlayerChangedWorldEvent): Unit = updatePlayerListName(e.getPlayer)

  private val playersTimePlayed: mutable.Map[Player, Long] = mutable.HashMap()

  @EventHandler def onPlayerJoin(e: PlayerJoinEvent): Unit =
    val playerDataEraser = PlayerDataEraser

    if playerDataEraser.cancelPlayerDataErasePlan(e.getPlayer.getUniqueId) then
      NoteMessage("Planned data removal canceled!\nThe data for your Minecraft account on Noxet was requested to be deleted. You have now aborted this.").send(e.getPlayer)

    playerDataEraser.performDataErasureCheck() // Check for passed data deletion requests when a player has joined.

    PlayerDataManager.clearCacheForUUID(e.getPlayer.getUniqueId)

    Message(s"§3■ ${TextBeautifier.beautify("Welcome to: ", false)}§b§l${TextBeautifier.beautify("noxet.org")}§3!").send(e.getPlayer)

    playersTimePlayed.put(e.getPlayer, System.currentTimeMillis)

    val playerDataManager = PlayerDataManager(e.getPlayer)

    val timesJoined = playerDataManager.get(PlayerDataManager.Attribute.TimesJoined).toInt + 1
    val secondsPlayed = playerDataManager.get(PlayerDataManager.Attribute.SecondsPlayed).toInt

    if secondsPlayed != 0 then
      Message(s"Your total playtime: §f${FancyTimeConverter.deltaSecondsToFancyTime(secondsPlayed)}").send(e.getPlayer)

    val fancyJoinAmount = timesJoined.toString +
      (timesJoined % 10, timesJoined % 100) match
      case (_, 11) | (_, 12) | (_, 13) => "th"
      case 1 => "st"
      case 2 => "nd"
      case 3 => "rd"
      case _ => "th"

    Message(s"§b${e.getPlayer.getDisplayName}§3 hopped on §bNoxet.org§3 for the §b§n${TextBeautifier.beautify(fancyJoinAmount)}§3 time.")
      .broadcast()
    e.setJoinMessage(null)

    playerDataManager.set(
      PlayerDataManager.Attribute.TimesJoined,
      playerDataManager.get(PlayerDataManager.Attribute.TimesJoined).toInt + 1
    )

    playerDataManager.save()

    if !playerDataManager.get(PlayerDataManager.Attribute.HasDoneCaptcha).toBoolean then
      Captcha(e.getPlayer).init()
      return

    if !e.getPlayer.getUniqueId == UsernameStorageManager.getUuidFromUsernameOrUuid(e.getPlayer.getName) then
      UsernameStorageManager
        .bindUsernameToUuid(e.getPlayer.getName, e.getPlayer.getUniqueId) // Correct username if changed (either entirely or just by different casing).

    Bukkit.getScheduler.scheduleSyncDelayedTask(NoxetServer.getPlugin, () =>
      getCurrentRealm(e.getPlayer) match
        case Some(value) =>
          setTemporaryInvulnerability(e.getPlayer)
          Message(s"§eYou are in §l${TextBeautifier.beautify(realm.toString, false)}§e.").addButton("Leave", ChatColor.RED, "Go to lobby", "hub").send(e.getPlayer)
          e.getPlayer.sendTitle(s"§e§l${TextBeautifier.beautify(realm.toString)}", "§3Type §b/hub §3to leave this realm.", 0, 120, 10)
        case None => goToHub(e.getPlayer) // Make sure player is at spawn.
      , 10)

    updatePlayerListName(e.getPlayer)

    CombatLoggingStorageManager().combatLogRejoin(e.getPlayer, getCurrentRealm(e.getPlayer))

    val incomingFriendRequests = PlayerDataManager(e.getPlayer).getListSize(PlayerDataManager.Attribute.IncomingFriendRequests)

    if incomingFriendRequests != 0 then
      Message(s"§6⚐ Incoming friend requests: §c$incomingFriendRequests")
        .addButton("Review", ChatColor.GREEN, "See who wants to befriend you", "friend incoming")
        .send(e.getPlayer)

  def updatePlayerListName(player: Player): Unit =
    val realm = getCurrentRealm(player)

    val playerListPrefix =
      if realm.isDefined then
        s"§e${TextBeautifier.beautify(realm.toString)}§r "
      else if MiniGameManager.isPlayerBusyInGame(player) then
        s"§3${TextBeautifier.beautify("in-game")}§r "
      else
        "§7"

    player.setPlayerListName(playerListPrefix + player.getDisplayName)

  @EventHandler def onPlayerQuit(e: PlayerQuitEvent): Unit =
    val player = e.getPlayer

    setPlayerMigrationStatus(player, false)
    Captcha.stopPlayerCaptcha(player)
    TeleportAsk.abortPlayerRelatedRequests(player)
    abortUnconfirmedPlayerRespawn(player)
    MsgConversation.clearActiveConversationModes(player)
    PlayerDataManager.clearCacheForUUID(player.getUniqueId)
    CombatLogging.triggerLocationDisband(player)
    Party.abandonPlayer(player)

    val playerDataManager = PlayerDataManager(player)

    val timePlayed = playersTimePlayed.remove(player)

    if timePlayed != null then
      playerDataManager.addInt(PlayerDataManager.Attribute.SecondsPlayed,
        ((System.currentTimeMillis - timePlayed) / 1000).toInt)

    playerDataManager.set(PlayerDataManager.Attribute.LastPlayed, System.currentTimeMillis / 1000)

    playerDataManager.save()

    Message(s"§f${player.getDisplayName}§7 left Noxet.org.").broadcast()
    e.setQuitMessage(null)

  @EventHandler def onPlayerDeath(e: PlayerDeathEvent): Unit =
    val player = e.getEntity

    CombatLogging.clearCombatLog(player)

    val realm = getCurrentRealm(player)

    val deathMessage = e.getDeathMessage
    e.setDeathMessage(null)

    val newDeathMessage = Message(s"§4☠ §c$deathMessage.")

    if realm.isEmpty then
      MiniGameManager.findPlayersGame(player) match
        case Some(inGame) => inGame.sendGameMessage(newDeathMessage)
        case None => ()
      return

    newDeathMessage.send(realm)

    setPlayerMigrationStatus(player, true)

    Bukkit.getScheduler.scheduleSyncDelayedTask(NoxetServer.getPlugin, () =>
      player.sendTitle("§4☠", "§cYou died...", 40, 60, 20)
      player.addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS, 40 + 60 + 20, 10, true, false))

      val totalPlays = 10
      for i <- 0 until totalPlays do
        QuickRunnable(() =>
          player.playSound(
            player.getLocation,
            Sound.ENTITY_CAMEL_DEATH,
            1,
            2 - 1.5f * i.toFloat / totalPlays)
          player.playSound(
            player.getLocation,
            Sound.BLOCK_BELL_USE,
            1 * i.toFloat / totalPlays,
            2 - 1.5f * i.toFloat / totalPlays)
        ).runTaskLater(NoxetServer.getPlugin, i * 5)
      , 2)

  @EventHandler def onPlayerSpawnLocation(e: PlayerSpawnLocationEvent): Unit =
    // If spawn is in a world, and the world is not in a realm.
    if e.getSpawnLocation.getWorld != null &&
      RealmManager.getRealmFromWorld(e.getSpawnLocation.getWorld).isEmpty
    then
      e.setSpawnLocation(RealmManager.getMainSpawn)

  @EventHandler def onPlayerRespawn(e: PlayerRespawnEvent): Unit =
    if RealmManager.getCurrentRealm(e.getPlayer).isDefined then
      setTemporaryInvulnerability(e.getPlayer)
    else if MiniGameManager.isPlayerBusyInGame(e.getPlayer) then
      return

    QuickRunnable(setPlayerMigrationStatus(e.getPlayer, true))
      .runTaskLater(NoxetServer.getPlugin, 20)

    e.setRespawnLocation(getRespawnLocation(e.getPlayer))

  @EventHandler def onPlayerAdvancementDone(e: PlayerAdvancementDoneEvent): Unit =
    e.getPlayer.getWorld.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false)

    val game = MiniGameManager.findPlayersGame(e.getPlayer)

    if game.isDefined then
      val advancementDisplay = e.getAdvancement.getDisplay
      if advancementDisplay != null && advancementDisplay.shouldAnnounceChat then
        game.sendGameMessage(
          Message(s"§e${e.getPlayer.getName} just advanced! §a§o${advancementDisplay.getTitle}: ${advancementDisplay.getDescription}"))

  @EventHandler def onServerListPing(e: ServerListPingEvent): Unit = e.setMotd(Motd.generateMotd)

  @EventHandler(priority = EventPriority.HIGH) def onAsyncPlayerChat(e: AsyncPlayerChatEvent): Unit =
    val shouldSend = !e.isCancelled && !Captcha.isPlayerDoingCaptcha(e.getPlayer)
    e.setCancelled(true)

    if shouldSend then
      val playerDataManager = PlayerDataManager(e.getPlayer)

      if !playerDataManager.get(PlayerDataManager.Attribute.SeenChatNotice).toBoolean then
        Message(
          s"§eHello, ${e.getPlayer.getName}!\n" +
            "Please read a message from us before you can chat.\n"
        ).addButton(
          "Read",
          ChatColor.GREEN,
          "Read a message from us to start talking",
          TemporaryCommand.ReadBeforeChat.getRawCommand()
        ).send(e.getPlayer)
        return

      if playerDataManager.get(PlayerDataManager.Attribute.MUTED).toBoolean then
        ErrorMessage(ErrorMessage.ErrorType.COMMON, "You are muted, and cannot chat at the moment!").send(e.getPlayer)
        return

      val realm = getCurrentRealm(e.getPlayer)

      val (game, channelName) =
        if realm.isDefined then
          (None, realm.toString)
        else if NoxetServer.ServerWorld.HUB.getWorld == e.getPlayer.getWorld then
          (None, "Hub")
        else
          MiniGameManager.findPlayersOrSpectatorsGame(e.getPlayer) match
            case Some(game) if game.hasStarted =>
              (game, if game.isPlayer(e.getPlayer) then "Game" else "Game Spectator")
            case _ => (None, None)

      val message = Message(
        channelName.map(name => s"§7${TextBeautifier.beautify(name)}§8⏵ ").getOrElse("") +
          s"§3${e.getPlayer.getDisplayName}§8→ §f${e.getMessage}")
      message.setPrefix(null)

      (game, realm) match
        // In game? Send message to the game.
        case (Some(g), _) => g.sendGameMessage(message)
        // In a realm? Send message to the realm.
        case (_, Some(r)) => message.send(r)
        // Neither in a game nor realm? Send only to the player's world.
        case _ => message.send(e.getPlayer.getWorld)

  @EventHandler def onPlayerCommandPreprocess(e: PlayerCommandPreprocessEvent): Unit =
    if Captcha.isPlayerDoingCaptcha(e.getPlayer) then
      e.setCancelled(true)
    else if TemporaryCommand.ConfirmBedSpawn.isMessageThisCommand(e) then
      e.setCancelled(true)

      if !unconfirmedPlayerRespawns.contains(e.getPlayer) then
        ErrorMessage(ErrorMessage.ErrorType.COMMON, "You cannot do this now.").send(e.getPlayer)
        return

      val newBedSpawn = unconfirmedPlayerRespawns.remove(e.getPlayer)
      e.getPlayer.setBedSpawnLocation(newBedSpawn)

      if e.getPlayer.getBedSpawnLocation != null &&
        newBedSpawn.getBlock.getBlockData.isInstanceOf[Bed] then
        Message("§aYour respawn location has been updated.").send(e.getPlayer)
      else
        ErrorMessage(ErrorMessage.ErrorType.COMMON, "Could not change your respawn location.").send(e.getPlayer)
    else if TemporaryCommand.UnderstandAnarchy.isMessageThisCommand(e) then
      val playerDataManager = PlayerDataManager(e.getPlayer)
      if !playerDataManager.get(PlayerDataManager.Attribute.HAS_UNDERSTOOD_ANARCHY).toBoolean then
        playerDataManager.set(PlayerDataManager.Attribute.HAS_UNDERSTOOD_ANARCHY, true).save()
        e.getPlayer.closeInventory()
        Message("§aThank you for understanding. We will not prompt you that again.").send(e.getPlayer)
        e.setCancelled(true)
    else if TemporaryCommand.ReadBeforeChat.isMessageThisCommand(e) then
      val playerDataManager = PlayerDataManager(e.getPlayer)
      if !playerDataManager.get(PlayerDataManager.Attribute.SEEN_CHAT_NOTICE).toBoolean then
        BookMenu(List(
          ComponentBuilder(
            s"§8Welcome to the §3${TextBeautifier.beautify("noxet")}§8 chat.\n" +
              "You can §0/msg§8 players to talk privately.\n" +
              "Make sure that you follow our rules!\n\n")
            .append(
              ComponentBuilder("§2§l■ I have read and understood this.")
                .event(ClickEvent(ClickEvent.Action.RUN_COMMAND,
                  TemporaryCommand.UnderstandChat.getSlashCommand))
                .event(HoverEvent(HoverEvent.Action.SHOW_TEXT, Text("Close this warning")))
                .create).create
        )).openMenu(e.getPlayer)

        e.setCancelled(true)
    else if TemporaryCommand.UnderstandChat.isMessageThisCommand(e) then
      val playerDataManager = PlayerDataManager(e.getPlayer)
      if !playerDataManager.get(PlayerDataManager.Attribute.SEEN_CHAT_NOTICE).toBoolean then
        playerDataManager.set(PlayerDataManager.Attribute.SEEN_CHAT_NOTICE, true).save()
        Message("§aYou can now chat!").send(e.getPlayer)
        e.getPlayer.closeInventory()

        e.setCancelled(true)

  def boostPlayer(player: Player): Unit =
    player.playSound(
      player.getLocation,
      Sound.ENTITY_FIREWORK_ROCKET_BLAST,
      1,
      (Math.random * 1.5 + 0.5).toFloat)

    val velocity = Vector(0, 0.5, 0).add(player.getLocation.getDirection.multiply(4))

    player.setVelocity(velocity)

    ActionBarMessage("§d↑ ↑ ↑ ↑").send(player)

  @EventHandler def onPlayerToggleFlight(e: PlayerToggleFlightEvent): Unit =
    if e.isFlying &&
      e.getPlayer.getWorld == NoxetServer.ServerWorld.HUB.getWorld &&
      e.getPlayer.getGameMode == GameMode.SURVIVAL
    then
      if Math.abs(e.getPlayer.getVelocity.getY) < 0.334 &&
        e.getPlayer.getLocation.getY < e.getPlayer.getWorld.getSpawnLocation.getY + 40
      then
        boostPlayer(e.getPlayer)
      e.setCancelled(true)

  @EventHandler def onBlockBreak(e: BlockBreakEvent): Unit =
    if NoxetServer.isWorldPreserved(e.getBlock.getWorld) then
      e.setCancelled(true)

  @EventHandler def onBlockPlace(e: BlockPlaceEvent): Unit =
    if NoxetServer.isWorldPreserved(e.getBlock.getWorld) then
      e.setCancelled(true)

  @EventHandler def onBlockForm(e: BlockFormEvent): Unit =
    if NoxetServer.isWorldPreserved(e.getBlock.getWorld) then
      e.setCancelled(true)

  @EventHandler def onBlockGrow(e: BlockGrowEvent): Unit =
    if NoxetServer.isWorldPreserved(e.getBlock.getWorld) then
      e.setCancelled(true)

  @EventHandler def onBlockExplode(e: BlockExplodeEvent): Unit =
    if NoxetServer.isWorldPreserved(e.getBlock.getWorld) then
      e.setCancelled(true)

  @EventHandler def onEntityInteract(e: EntityInteractEvent): Unit =
    if NoxetServer.isWorldPreserved(e.getBlock.getWorld) then
      e.setCancelled(true)

  @EventHandler def onBlockFade(e: BlockFadeEvent): Unit =
    if NoxetServer.isWorldPreserved(e.getBlock.getWorld) then
      e.setCancelled(true)

  @EventHandler def onBlockDamage(e: BlockDamageEvent): Unit =
    if NoxetServer.isWorldPreserved(e.getBlock.getWorld) then
      e.setCancelled(true)

  @EventHandler def onBlockBurn(e: BlockBurnEvent): Unit =
    if NoxetServer.isWorldPreserved(e.getBlock.getWorld) then
      e.setCancelled(true)

  @EventHandler def onBlockFertilize(e: BlockFertilizeEvent): Unit =
    if NoxetServer.isWorldPreserved(e.getBlock.getWorld) then
      e.setCancelled(true)

  @EventHandler def onBlockIgnite(e: BlockIgniteEvent): Unit =
    if NoxetServer.isWorldPreserved(e.getBlock.getWorld) then
      e.setCancelled(true)

  @EventHandler def onBlockSpread(e: BlockSpreadEvent): Unit =
    if NoxetServer.isWorldPreserved(e.getBlock.getWorld) then
      e.setCancelled(true)

  @EventHandler def onPlayerDropItem(e: PlayerDropItemEvent): Unit =
    if NoxetServer.isWorldPreserved(e.getBlock.getWorld) then
      e.setCancelled(true)

  @EventHandler def onEntityPickupItem(e: EntityPickupItemEvent): Unit =
    if NoxetServer.isWorldPreserved(e.getItem.getWorld) then
      e.setCancelled(true)

  @EventHandler def onItemSpawn(e: ItemSpawnEvent): Unit =
    if NoxetServer.isWorldPreserved(e.getLocation.getWorld) then
      e.setCancelled(true)

  @EventHandler def onPlayerInteract(e: PlayerInteractEvent): Unit =
    if e.getAction == Action.PHYSICAL && e.getPlayer.getWorld == NoxetServer.ServerWorld.HUB.getWorld then
      boostPlayer(e.getPlayer)
      return

    if e.getItem != null && e.getAction != Action.PHYSICAL then
      if e.getItem.equals(HubInventorySetup.gameNavigator) then
        GameNavigationMenu.openInventory(e.getPlayer)
        e.setCancelled(true)
      else if e.getItem.equals(HubInventorySetup.socialNavigator) then
        SocialMenu(e.getPlayer).openInventory(e.getPlayer)
        e.setCancelled(true)
      else if e.getItem.equals(HubInventorySetup.settings) then
        SettingsMenu.openInventory(e.getPlayer)
        e.setCancelled(true)

    if e.getAction == Action.RIGHT_CLICK_AIR && e.getMaterial == Material.ENDER_CHEST then
      val realm = RealmManager.getCurrentRealm(e.getPlayer)

      if realm.isDefined && realm.doesAllowTeleportationMethods then
        e.getPlayer.performCommand("enderchest")

    if NoxetServer.isWorldPreserved(e.getPlayer.getWorld) &&
      !(e.getAction == Action.RIGHT_CLICK_BLOCK &&
        e.getClickedBlock != null &&
        e.getClickedBlock.getType == Material.ENDER_CHEST)
    then
      e.setCancelled(true)
      return

    if e.getAction == Action.LEFT_CLICK_AIR &&
      ChickenLeg.isPlayerChickenLeg(e.getPlayer)
    then
      ChickenLeg.summonChickenLeg(e.getPlayer)

  @EventHandler def onEntityChangeBlock(e: EntityChangeBlockEvent): Unit =
    if NoxetServer.isWorldPreserved(e.getBlock.getWorld) then
      e.setCancelled(true)

  private val invulnerablePlayers: Set[Player] = HashSet()

  def setTemporaryInvulnerability(player: Player, secondsInvulnerable: Int = 6): Unit =
    if invulnerablePlayers.contains(player) ||
      NoxetServer.isWorldSafeZone(player.getWorld)
    then return

    invulnerablePlayers.add(player)

    for secondsLeft <- secondsInvulnerable until 0 by -1 do
      QuickRunnable(
        ActionBarMessage(s"§e§lINVULNERABLE §c${secondsLeft}s").send(player)
      ).runTaskLater(NoxetServer.getPlugin, (secondsInvulnerable - secondsLeft) * 20)

    QuickRunnable(() =>
      ActionBarMessage("§cYou are no longer invulnerable.").send(player)
      invulnerablePlayers.remove(player)
    ).runTaskLater(NoxetServer.getPlugin, ticksInvulnerable)

  private val recentlyDamageRespawnedPlayers: Set[Player] = HashSet()

  private val SafeZoneWorldDamageCauses = List(DamageCause.VOID, DamageCause.SUFFOCATION)

  @EventHandler def onEntityDamage(e: EntityDamageEvent): Unit =
    if NoxetServer.isWorldSafeZone(e.getEntity.getWorld) then
      e.getEntity match
        case p: Player if
          SafeZoneWorldDamageCauses.contains(e.getCause) &&
            !recentlyDamageRespawnedPlayers.contains(p)
        =>
          recentlyDamageRespawnedPlayers.add(p)
          QuickRunnable(recentlyDamageRespawnedPlayers.remove(p))
            .runTaskLater(NoxetServer.getPlugin, 120)
          goToSpawn(p)
        case _ => ()
      e.setCancelled(true)
    else e.getEntity match
      case p: Player if invulnerablePlayers.contains(p) => e.setCancelled(true)
      case _ => ()

  @EventHandler def onInventoryClick(e: InventoryClickEvent): Unit =
    if e.getWhoClicked.getWorld == NoxetServer.ServerWorld.HUB.getWorld ||
      invulnerablePlayers.contains(e.getWhoClicked.asInstanceOf[Player])
    then
      e.setCancelled(true)

  @EventHandler def onPlayerSwapHandItems(e: PlayerSwapHandItemsEvent): Unit =
    if e.getPlayer.getWorld == NoxetServer.ServerWorld.HUB.getWorld ||
      invulnerablePlayers.contains(e.getPlayer)
    then
      e.setCancelled(true)

  @EventHandler def onPlayerInteractAtEntity(e: PlayerInteractAtEntityEvent): Unit =
    if invulnerablePlayers.contains(e.getPlayer) then
      e.setCancelled(true)

  @EventHandler(priority = EventPriority.HIGHEST) def onEntityDamageByEntity(e: EntityDamageByEntityEvent): Unit =
    (e.getEntity, e.getDamager) match
      case (victim: Player, offender: Player) if !e.isCancelled =>
        CombatLogging.triggerCombatLog(victim)
        CombatLogging.triggerCombatLog(offender)
      case _ => ()

  @EventHandler def onEntityPortalEnter(e: EntityPortalEnterEvent): Unit =
    (e.getEntity, e.getLocation.getWorld) match
      case (p: Player, NoxetServer.ServerWorld.HUB.getWorld) =>
        goToSpawn(p)
        QuickRunnable(GameNavigationMenu.openInventory(p))
          .runTaskLater(NoxetServer.getPlugin, 5)

  @EventHandler def onEntityPortal(e: EntityPortalEvent): Unit =
    if MiniGameController.isGameWorld(e.getFrom.getWorld) then
      e.setCancelled(true)
      return

    handlePortalTeleport(e.getFrom, e.getTo)

  @EventHandler def onPlayerPortal(e: PlayerPortalEvent): Unit =
    if MiniGameController.isGameWorld(e.getFrom.getWorld) then
      e.setCancelled(true)
      return

    handlePortalTeleport(e.getFrom, e.getTo)

  private def handlePortalTeleport(from: Option[Location], to: Option[Location]): Unit =
    if to.isEmpty then return

    val realm = getRealmFromWorld(from.getWorld) match
      case Some(x) => x
      case None => return // No realm, we don't have to handle this.

    to.setWorld(realm.getWorld(to.getWorld.getEnvironment match
      case NORMAL => NoxetServer.WorldFlag.Overworld
      case NETHER => NoxetServer.WorldFlag.Nether
      case THE_END => NoxetServer.WorldFlag.TheEnd
    ))

  private val unconfirmedPlayerRespawns: Map[Player, Location] = HashMap()

  def abortUnconfirmedPlayerRespawn(player: Player): Unit =
    if unconfirmedPlayerRespawns.remove(player).isDefined then
      Message("§cYour respawn location was not changed.").send(player)

  @EventHandler def onPlayerSpawnChange(e: PlayerSpawnChangeEvent): Unit =
    val oldBedSpawn = e.getPlayer.getBedSpawnLocation

    if oldBedSpawn != null &&
      e.getNewSpawn != null &&
      oldBedSpawn.getBlock.getLocation.distance(e.getNewSpawn) > 2 &&
      e.getCause == PlayerSpawnChangeEvent.Cause.BED
    then
      unconfirmedPlayerRespawns.put(e.getPlayer, e.getNewSpawn)

      QuickRunnable(ActionBarMessage("§cYour spawn location was §lNOT§c changed! Read chat.").send(e.getPlayer))
        .runTaskLater(NoxetServer.getPlugin, 0)

      WarningMessage("You already have a respawn location. Replace it?")
        .addButton(
          "Replace",
          ChatColor.RED,
          "Set this as your new spawn",
          TemporaryCommand.ConfirmBedSpawn.getRawCommand)
        .send(e.getPlayer)

      getCurrentRealm(e.getPlayer) match
        case Some(realm) if realm.allowTeleportationMethods =>
          NoteMessage(s"In $realm, you can save locations which you can teleport to, simply with the /home command.")
            .addButton(
              "Add home here",
              ChatColor.GREEN,
              "Add a home to easily get here",
              "home set ?"
            )
            .send(e.getPlayer)
        case None => ()

      QuickRunnable(unconfirmedPlayerRespawns.remove(e.getPlayer))
        .runTaskLater(NoxetServer.getPlugin, 20 * 30)

      e.setCancelled(true)

  private val recentlyKickedToRemoveCheats: Set[UUID] = HashSet()

  def setPlayerRecentlyKickedToRemoveCheats(uuid: UUID): Unit =
    recentlyKickedToRemoveCheats.add(uuid)

    QuickRunnable(recentlyKickedToRemoveCheats.remove(uuid))
      .runTaskLater(NoxetServer.getPlugin, 20 * 10)

  @EventHandler def onAsyncPlayerPreLogin(e: AsyncPlayerPreLoginEvent): Unit =
    if recentlyKickedToRemoveCheats.contains(e.getUniqueId) then
      e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, "§cThat was a little fast, wasn't it? Wait a few seconds.")