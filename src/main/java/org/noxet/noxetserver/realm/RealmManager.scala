package org.noxet.noxetserver.realm

import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import org.noxet.noxetserver.commands.teleportation.TeleportAsk
import org.noxet.noxetserver.menus.book.BookMenu
import org.noxet.noxetserver.messaging.Message
import org.noxet.noxetserver.minigames.MiniGameManager
import org.noxet.noxetserver.playerstate.PlayerState
import org.noxet.noxetserver.playerstate.PlayerState.PlayerStateType
import org.noxet.noxetserver.util.TextBeautifier
import org.noxet.noxetserver.{Events, NoxetServer}

import java.util.Collections
import scala.collection.mutable

object RealmManager:
    enum MigrationType:
        case MigrateTo, ImmigrateFrom

    /**
     * Contains all registered realms and their properties.
     */
    enum Realm(
                val displayName: String,
                val playerStateType: PlayerStateType,
                val allowTeleportationMethods: Boolean,
                onMigrateToCallback: Option[Player => Unit],
                onMigrateFromCallback: Option[Player => Unit]):
        case Smp extends Realm("SMP", PlayerStateType.SMP, true, None, None)
        case Anarchy extends Realm("Anarchy Island", PlayerStateType.ANARCHY, false, Some((player: Player) => {
            if !hasPlayerUnderstoodAnarchy(player) then
                val realmBefore = getCurrentRealm(player) // We know this is Realm.ANARCHY, but it is not yet defined in this scope.

                val bookMenu = BookMenu(Collections.singletonList(
                    ComponentBuilder(
                        """§8Cheats are allowed in the §cAnarchy Island§8 realm and §lONLY§8 there!\n
                          |Using cheats outside of this realm will get you banned.\n
                          |§cMalicious cheats (that produce lag, spam, etc.) are not allowed.\n\n""".stripMargin)
                      .append(
                          ComponentBuilder("§2§l■ Thanks, I'll remember this.")
                            .event(ClickEvent(ClickEvent.Action.RUN_COMMAND, Events.TemporaryCommand.UNDERSTAND_ANARCHY.getSlashCommand))
                            .event(HoverEvent(HoverEvent.Action.SHOW_TEXT, Text("Close this warning"))).create
                      ).create()
                ))

                Events.setTemporaryInvulnerability(player, 25)

                new BukkitRunnable {
                    override def run(): Unit =
                        if player.isOnline && realmBefore == getCurrentRealm(player) && !hasPlayerUnderstoodAnarchy(player) then
                            bookMenu.openMenu(player)
                        else
                            this.cancel()
                }.runTaskTimer(NoxetServer.getPlugin, 20, 20)

                new BukkitRunnable() {
                    override def run(): Unit =
                        if player.isOnline && realmBefore == getCurrentRealm(player) && !hasPlayerUnderstoodAnarchy(player) then
                            player.kickPlayer("§cYou did not confirm.")
                }.runTaskLater(NoxetServer.getPlugin, 20 * 25)
        }), Some((player: Player) => {
            if !MiniGameManager.isPlayerBusyInGame(player) then
                Events.setPlayerRecentlyKickedToRemoveCheats(player.getUniqueId)

                new BukkitRunnable {
                    override def run(): Unit =
                        player.kickPlayer(
                            "§a☺ This is just a helping friendly reminder. You are §nnot§a banned.\n\n" +
                              "§7⚠ §lEXITING ANARCHISTIC REGION §7⚠\n" +
                              "§c" + TextBeautifier.beautify("You were disconnected because you left Anarchy Island.") + "\n" +
                              TextBeautifier.beautify("Cheats are not allowed where you are heading.") + "\n" +
                              "§e" + TextBeautifier.beautify("Close any advantage-granting modifications before rejoining our server, to avoid a ban.")
                        )
                }.runTaskLater(NoxetServer.getPlugin, 20)
        }))
        case Canvas extends Realm("Canvas", PlayerStateType.CANVAS, true, None, None)

        def getWorld(worldFlag: NoxetServer.WorldFlag): Option[World] =
            NoxetServer.ServerWorld.values.find(w => w.getRealm == this && w.getWorldFlag == worldFlag).map(_.getWorld)

        def getSpawnLocation: Option[Location] =
            RealmDataManager().getSpawnLocation(this)
              .orElse(getWorld(NoxetServer.WorldFlag.NEUTRAL).map(_.getSpawnLocation))
              .orElse(getWorld(NoxetServer.WorldFlag.OVERWORLD).map(_.getSpawnLocation))

        def getWorlds: List[World] = NoxetServer.ServerWorld.values.toList.filter(_.getRealm == this).map(_.getWorld)

        def getPlayers: Set[Player] = getWorlds.iterator.flatMap(_.getPlayers)

        def onMigration(player: Player, direction: MigrationType): Unit =
            (direction, onMigrateToCallback, onMigrateFromCallback) match
                case (MigrationType.MigrateTo, Some(callback), _) => callback(player)
                case (MigrationType.ImmigrateFrom, _, Some(callback)) => callback(player)

        def getPlayerCount: Int = getPlayers.size

    private val migratingPlayers = mutable.HashSet[Player]()

    def setPlayerMigrationStatus(player: Player, migrating: Boolean): Unit =
        if migrating then
            migratingPlayers.add(player)
        else
            migratingPlayers.remove(player)

    def isPlayerMigrating(player: Player): Boolean = migratingPlayers.contains(player)

    /**
     * Gets the realm that the world belongs to.
     *
     * @param world The world whose realm to return
     * @return The realm that the world is in, None if not a realm
     */
    def getRealmFromWorld(world: World): Option[Realm] = NoxetServer.ServerWorld.values.find(_.getWorld == world).map(_.getRealm)

    /**
     * Gets the realm that the player is currently in.
     *
     * @param player The player whose realm to return
     * @return The realm that the player is in, None if not in a realm
     */
    def getCurrentRealm(player: Player): Option[Realm] = getRealmFromWorld(player.getWorld)

    /**
     * Prepares player to move to realm by saving current state, restoring realm state and performing the teleport.
     *
     * @param player The player to move to another realm
     * @param toRealm The realm to move the player to
     */
    def migrateToRealm(player: Player, toRealm: Option[Realm]): Unit =
        if isPlayerMigrating(player) then return

        // Save state in current realm:

        val fromRealm = getCurrentRealm(player)

        if toRealm == fromRealm then return // Already in that realm. Do nothing.

        TeleportAsk.abortPlayerRelatedRequests(player)
        Events.abortUnconfirmedPlayerRespawn(player)
        CombatLoggingStorageManager.combatLogRejoin(player, getCurrentRealm(player))

        toRealm match
            case Some(r) => Message(s"Entering §e${r.displayName}§7 ...").send(player)
            case None => ()

        fromRealm match
            case Some(r) =>
                // Source location is a realm.
                PlayerState.saveState(player, r.playerStateType) // Save state in old location's realm.
                r.onMigration(player, MigrationType.ImmigrateFrom)
                Message(s"§f${player.getDisplayName}§7 left §f${r.displayName}§7.").send(r)
            case None => PlayerState.saveState(player, PlayerStateType.GLOBAL) // In non-realm world. Using global state.

        // Migrate to realm:

        setPlayerMigrationStatus(player, true)

        new BukkitRunnable {
            override def run(): Unit = setPlayerMigrationStatus(player, false)
        }.runTaskLater(NoxetServer.getPlugin, 60) // 3 seconds of migration margin.

        toRealm match
            case Some(r) => // Destination is a realm
                Events.setTemporaryInvulnerability(player)

                PlayerState.restoreState(player, r.playerStateType) // Restores player state (including initial reset), and teleports to last location (in a world belonging to the realm).

                r.getSpawnLocation match
                    case Some(spawnLocation) if !PlayerState.hasState(player, r.playerStateType) =>
                        player.teleport(spawnLocation) // Teleport to spawn (first join).
                    case None => throw new Exception(s"realm $r has no valid spawn location")

                r.onMigration(player, MigrationType.MigrateTo)
                Message(s"§f${player.getDisplayName}§7 joined §f${r.displayName}§7.").send(r)
            case None => PlayerState.restoreState(player, PlayerStateType.GLOBAL) // Regular world. Load global state.

        setPlayerMigrationStatus(player, false)
        Events.updatePlayerListName(player)

    def getMainSpawn: Location = RealmDataManager.getSpawnLocation(null).getOrElse(NoxetServer.ServerWorld.HUB.getWorld.getSpawnLocation)

    def getSpawnLocation(player: Player): Location =
        val realm = getCurrentRealm(player).map(_.getSpawnLocation).getOrElse(getMainSpawn)

    def getRespawnLocation(player: Player): Location =
        Option(player.getBedSpawnLocation).getOrElse(getSpawnLocation(player))

    /**
     * Send a player to the spawn. If in a realm, sent to realm spawn. If not in a realm, sent to world spawn.
     *
     * @param player The player to send to spawn
     */
    def goToSpawn(player: Player): Unit =
        player.teleport(getSpawnLocation(player))
        val spawnName = getCurrentRealm(player) match
            case Some(realm) => s"§f§l${realm.displayName}§7"
            case None => "hub"
        ActionBarMessage(s"§7You have been sent to $spawnName spawn!").send(player)

    /**
     * Send a player to the central hub.
     *
     * @param player The player to send to hub
     */
    def goToHub(player: Player): Unit =
        PlayerState.prepareHubState(player)
        player.teleport(getMainSpawn)

        player.playSound(player.getLocation, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1, 0.5f)
        player.sendTitle(
            s"§b§l${TextBeautifier.beautify("no")}§3§l${TextBeautifier.beautify("x")}§b§l${TextBeautifier.beautify("et")}",
            "§eWelcome to the Noxet.org Network.",
            0, 60, 5)

        player.spawnParticle(
            Particle.EXPLOSION_HUGE,
            player.getLocation.add(
                player.getLocation.getDirection
                  .multiply(2)
                  .add(org.bukkit.util.Vector(0, 1, 0)),
                10
            ))

    def hasPlayerUnderstoodAnarchy(player: Player): Boolean =
        PlayerDataManager(player).get(PlayerDataManager.Attribute.HAS_UNDERSTOOD_ANARCHY).asInstanceOf[Boolean]