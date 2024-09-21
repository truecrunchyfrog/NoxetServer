package org.noxet.noxetserver.commands.realms.smp

import org.bukkit.block.Biome.*
import org.bukkit.command.{Command, CommandExecutor, CommandSender}
import org.bukkit.entity.Player
import org.bukkit.potion.{PotionEffect, PotionEffectType}
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.{Bukkit, Location, Sound, World}
import org.noxet.noxetserver.NoxetServer
import org.noxet.noxetserver.commands.{PlayerCommandExecutor, RegisteredCommand}
import org.noxet.noxetserver.messaging.{ErrorMessage, Message}
import org.noxet.noxetserver.realm.RealmManager
import org.noxet.noxetserver.realm.RealmManager.getCurrentRealm
import org.noxet.noxetserver.util.{QuickRunnable, TeleportUtil}

import java.text.DecimalFormat
import scala.annotation.tailrec
import scala.collection.mutable

object Wild extends PlayerCommandExecutor, RegisteredCommand("wild", this):
  private val recentlyTeleported = mutable.HashSet[Player]()
  private val wildBiomesExceptions = List(
    OCEAN,
    FROZEN_OCEAN,
    COLD_OCEAN,
    LUKEWARM_OCEAN,
    WARM_OCEAN,
    DEEP_OCEAN,
    DEEP_COLD_OCEAN,
    DEEP_LUKEWARM_OCEAN,
    DEEP_FROZEN_OCEAN,
    RIVER,
    FROZEN_RIVER
  )

  @tailrec
  def findWildLocation(world: World, attempts: Int): Option[Location] =
    if attempts < 1 then return None

    val (x, z) = (wildXZValue, wildXZValue)
    val loc = Location(
      world,
      x, world.getHighestBlockYAt(x, z), z
    )

    if !wildBiomesExceptions.contains(world.getBiome(loc)) &&
      TeleportUtil.isLocationTeleportSafe(loc)
    then Some(loc)
    else findWildLocation(world, attempts - 1)

  override def onPlayerCommand(player: Player): Boolean =
    if recentlyTeleported.contains(player) then
      ErrorMessage(ErrorMessage.ErrorType.Common,
        "You recently got a wilderness teleport. You must wait 2 minutes between requests.").send(player)
      return true

    getCurrentRealm(player) match
      case Some(realm) if realm.allowTeleportationMethods =>
        Message("Wilderness teleportation commencing. Please wait ...").send(player)

        QuickRunnable(() =>
          findWildLocation(realm.getWorld(NoxetServer.WorldFlag.Overworld), 300) match
            case Some(loc) =>
              player.teleport(loc)
              QuickRunnable(() =>
                if loc.getChunk.isLoaded then
                  wildIntro(player)
                  this.cancel()
              ).runTaskTimer(NoxetServer.getPlugin, 0, 20)
            case None =>
              ErrorMessage(ErrorMessage.ErrorType.Common,
                "Uh oh. We could not find a suiting place for you. Please try again.")
                .send(player)
        ).runTaskLater(NoxetServer.getPlugin, 0)

        recentlyTeleported.add(player)

        QuickRunnable(recentlyTeleported.remove(player))
          .runTaskLater(NoxetServer.getPlugin, 20 * 60 * 2)
        true
      case _ =>
        ErrorMessage(ErrorMessage.ErrorType.Common, "Wilderness teleportation is not supported here.").send(player)
        true


  private def wildIntro(player: Player): Unit =
    val loc = player.getLocation

    QuickRunnable(() =>
      Message("Welcome to the wild!").send(player)

      player.addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS, 420, 10, false, false))
      player.addPotionEffect(PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 420, 100, false, false))

      player.playSound(loc, Sound.ITEM_GOAT_HORN_SOUND_2, 1, 0.5f)

      player.sendTitle(
        "§2§lINTO THE WILDERNESS!",
        s"§f(§a§l${loc.getX.toInt}§7; §a§l${loc.getZ.toInt}§f)",
        0, 200, 0)
    ).runTaskLater(NoxetServer.getPlugin, 60)

    QuickRunnable(() =>
      player.playSound(loc, Sound.ITEM_GOAT_HORN_SOUND_3, 1, 0.5f)
      player.sendTitle(
        "§e§l" +
          DecimalFormat("#,###").format(loc.distance(loc.clone.zero).toInt),
        "§6blocks away from (0; 0).", 0, 200, 0)
    ).runTaskLater(NoxetServer.getPlugin, 60 + 200)

  private val wildBound = 15 * math.pow(10, 3).toInt

  private def wildXZValue: Int =
    Random().nextInt(-wildBound, wildBound)