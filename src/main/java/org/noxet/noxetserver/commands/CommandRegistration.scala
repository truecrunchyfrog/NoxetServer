package org.noxet.noxetserver.commands

import org.noxet.noxetserver.NoxetServer
import org.noxet.noxetserver.commands.admin.*
import org.noxet.noxetserver.commands.admin.realm.{ResetRealmSpawn, SetRealmSpawn}
import org.noxet.noxetserver.commands.debug.{ClearPlayerDataCache, FakeCombatLog, PreloadMiniGameWorld, WhereAmI}
import org.noxet.noxetserver.commands.games.creepersweeper.{ClearCreeperSweeperStats, CreeperSweeper}
import org.noxet.noxetserver.commands.games.misc.{Game, GameSelector, PartyCommand}
import org.noxet.noxetserver.commands.misc.{CanvasWorld, ChickenLeg, EnderChest}
import org.noxet.noxetserver.commands.realms.anarchy.Anarchy
import org.noxet.noxetserver.commands.realms.smp.{SMP, Wild}
import org.noxet.noxetserver.commands.social.*
import org.noxet.noxetserver.commands.teleportation.{Home, Hub, Spawn, TeleportAsk}

object CommandRegistration:
  private val commandDefinitions = List(
    "smp" -> new SMP,
    "anarchy" -> new Anarchy,
    "hub" -> new Hub,
    "spawn" -> new Spawn,
    "canvas-world" -> new CanvasWorld,

    "wild" -> new Wild,

    "tpa" -> new TeleportAsk,

    "whereami" -> new WhereAmI,

    "games" -> new GameSelector,

    "home" -> new Home,

    "chickenleg" -> new ChickenLeg,

    "doas" -> new DoAs,

    "mute" -> new Mute,
    "unmute" -> new Unmute,

    "msg" -> new MsgConversation,

    "toggle-preserve" -> new TogglePreserve,

    "loop" -> new Loop,

    "set-realm-spawn" -> new SetRealmSpawn,
    "reset-realm-spawn" -> new ResetRealmSpawn,

    "friend" -> new Friend,
    "block" -> new Block,
    "unblock" -> new Unblock,
    "block-list" -> new BlockList,

    "clear-player-data-cache" -> new ClearPlayerDataCache,

    "fake-combat-log" -> new FakeCombatLog,

    "creeper-sweeper" -> new CreeperSweeper,
    "clear-creeper-sweeper-stats" -> new ClearCreeperSweeperStats,

    "enderchest" -> new EnderChest,

    "game" -> new Game,

    "party" -> new PartyCommand,

    "preload-mini-game-world" -> new PreloadMiniGameWorld,
  )

  // Remember to define command in src/main/resources/plugin.yml, too!

  def registerCommands(): Unit =
    for
      cmdDef <- commandDefinitions
      command = NoxetServer.getPlugin.getCommand(cmdDef(0))
    do
      command match
        case null => NoxetServer.logSevere(s"Command definition: '${cmdDef(0)}' for '${cmdDef(1)}' not found!")
        case _ => command.setExecutor(cmdDef(1))