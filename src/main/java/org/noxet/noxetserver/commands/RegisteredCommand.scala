package org.noxet.noxetserver.commands

import org.noxet.noxetserver.NoxetServer
import org.noxet.noxetserver.commands.admin.*
import org.noxet.noxetserver.commands.admin.realm.{ResetRealmSpawn, SetRealmSpawn}
import org.noxet.noxetserver.commands.debug.{ClearPlayerDataCache, FakeCombatLog, PreloadMiniGameWorld, WhereAmI}
import org.noxet.noxetserver.commands.games.creepersweeper.{ClearCreeperSweeperStats, CreeperSweeper}
import org.noxet.noxetserver.commands.games.misc.{Game, GameSelector, PartyCommand}
import org.noxet.noxetserver.commands.misc.{CanvasWorld, ChickenLeg, EnderChest}
import org.noxet.noxetserver.commands.realms.anarchy.Anarchy
import org.noxet.noxetserver.commands.realms.smp.{Smp, Wild}
import org.noxet.noxetserver.commands.social.*
import org.noxet.noxetserver.commands.teleportation.{Home, Hub, Spawn, TeleportAsk}

/**
 * Extend your CommandExecutor class with this to register it as a command.
 * @note Remember to define command in src/main/resources/plugin.yml, too!
  */
trait RegisteredCommand(commandName: String, self: CommandExecutor):
  Option(NoxetServer.getPlugin.getCommand(commandName)) match
    case Some(command) => command.setExecutor(self)
    case None => NoxetServer.logSevere(s"Command definition: '$self' for '$commandName' not found!")