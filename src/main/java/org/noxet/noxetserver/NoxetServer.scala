package org.noxet.noxetserver

import org.bukkit.World.Environment
import org.bukkit.generator.ChunkGenerator
import org.bukkit.generator.ChunkGenerator.{BiomeGrid, ChunkData}
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.{World, WorldCreator, WorldType}
import org.noxet.noxetserver.commands.CommandRegistration
import org.noxet.noxetserver.messaging.Motd
import org.noxet.noxetserver.realm.RealmManager.Realm

import java.io.File
import java.util.Random

class NoxetServer extends JavaPlugin:
    override def onEnable(): Unit =
        NoxetServer.plugin = Some(this)

        getServer.getPluginManager.registerEvents(Events(), this)

        Motd.loadQuotes()

        NoxetServer.logInfo("Loading commands...")
        CommandRegistration.registerCommands()

        NoxetServer.logInfo("Noxet plugin is ready.")

    override def onDisable(): Unit = NoxetServer.logInfo("Noxet plugin stopped.")

    def getPluginDirectory: File =
        val dir = getDataFolder

        if (!dir.mkdir && (!dir.exists || !dir.isDirectory))
            throw new RuntimeException("Cannot create plugin directory.")

        dir

object NoxetServer:
    enum WorldFlag:
        case Neutral, Overworld, Nether, TheEnd, Flat, _Void

    enum ServerWorld(val worldName: String, val realm: Option[Realm], val preservedWorld: Boolean, val safeZone: Boolean, val flag: WorldFlag):
        case Hub extends ServerWorld("hub", None, true, true, WorldFlag.Neutral)
        case SmpSpawn extends ServerWorld("smp_spawn", Some(Realm.SMP), true, true, WorldFlag.Neutral)
        case SmpWorld extends ServerWorld("smp_world", Some(Realm.SMP), false, false, WorldFlag.Overworld)
        case SmpNether extends ServerWorld("smp_nether", Some(Realm.SMP), false, false, WorldFlag.Nether)
        case SmpEnd extends ServerWorld("smp_end", Some(Realm.SMP), false, false, WorldFlag.TheEnd)
        case AnarchyWorld extends ServerWorld("anarchy", Some(Realm.ANARCHY), false, false, WorldFlag.Overworld)
        case AnarchyNether extends ServerWorld("anarchy_nether", Some(Realm.ANARCHY), false, false, WorldFlag.Nether)
        case AnarchyEnd extends ServerWorld("anarchy_end", Some(Realm.ANARCHY), false, false, WorldFlag.TheEnd)

        case CanvasWorld extends ServerWorld("canvas", Some(Realm.CANVAS), false, true, WorldFlag._Void)

        private def getWorldCreator: WorldCreator =
            val worldCreator = WorldCreator(worldName)

            var environment = World.Environment.NORMAL

            flag match
                case WorldFlag.Nether => environment = Environment.NETHER
                case WorldFlag.TheEnd => environment = Environment.THE_END
                case WorldFlag.Flat =>
                    worldCreator.`type`(WorldType.FLAT)
                    worldCreator.generateStructures(false)
                case _Void =>
                    worldCreator.generator(new ChunkGenerator:
                        override def generateChunkData(world: World, random: Random, x: Int, z: Int, biome: BiomeGrid): ChunkData = createChunkData(world)
                    )

            worldCreator.environment(environment)

        def getWorld: World = NoxetServer.getPlugin.getServer.createWorld(getWorldCreator)

    private var plugin: Option[NoxetServer] = None

    def getPlugin: NoxetServer = plugin.get

    def shouldAllowWorldPreservation: Boolean = true

    def logInfo(message: String): Unit = getPlugin.getLogger.info(message)

    def logWarning(message: String): Unit = getPlugin.getLogger.warning(message)

    def logSevere(message: String): Unit = getPlugin.getLogger.severe(message)

    def isWorldPreserved(world: World): Boolean =
        ServerWorld.values.find(_.getWorld == world).exists(_.preservedWorld)

    def isWorldSafeZone(world: World): Boolean =
        ServerWorld.values.find(_.getWorld == world).exists(_.safeZone)