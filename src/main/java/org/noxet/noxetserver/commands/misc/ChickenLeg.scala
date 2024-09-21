package org.noxet.noxetserver.commands.misc

import org.bukkit.command.Command
import org.bukkit.entity.{Entity, EntityType, Player}
import org.bukkit.scheduler.BukkitRunnable
import org.noxet.noxetserver.NoxetServer
import org.noxet.noxetserver.commands.RegisteredCommand
import org.noxet.noxetserver.commands.admin.OperatorPlayerCommandExecutor
import org.noxet.noxetserver.messaging.{ErrorMessage, Message}
import org.noxet.noxetserver.util.{QuickRunnable, Sample}

import scala.collection.mutable

object ChickenLeg extends OperatorPlayerCommandExecutor, RegisteredCommand("chickenleg", this):
  private val chickenLegPlayers = mutable.HashSet[Player]()

  override def onOperatorPlayerCommand(player: Player): Boolean =
    if chickenLegPlayers.add(player) then
      Message("ChickenLeg has been enabled.").send(player)
    else
      stopChickenLeg(player)

    true

  def isPlayerChickenLeg(player: Player): Boolean = chickenLegPlayers.contains(player)

  def stopChickenLeg(player: Player): Unit =
    chickenLegPlayers.remove(player)
    Message("no longer chicken leg :(").send(player)

  def summonChickenLeg(player: Player): Unit =
    val bomber = player.getWorld.spawnEntity(player.getLocation, EntityType.values.toList.sampleOne(Random()))

    bomber.setVelocity(player.getLocation.getDirection.multiply(6))

    QuickRunnable(() =>
      if !bomber.isDead then
        bomber.getWorld.createExplosion(bomber.getLocation, 3, true)
        bomber.remove()
    ).runTaskLater(NoxetServer.getPlugin, 60)