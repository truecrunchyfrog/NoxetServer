package org.noxet.noxetserver.combatlogging

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.{BukkitRunnable, BukkitTask}
import org.noxet.noxetserver.NoxetServer
import org.noxet.noxetserver.messaging.Message
import org.noxet.noxetserver.minigames.MiniGameManager

import scala.collection.mutable

object CombatLogging:
  private val combatLogged: mutable.Map[Player, BukkitTask] = mutable.Map()
  private val combatLogTimeout = 20 * 20

  def triggerCombatLog(player: Player): Unit =
    if MiniGameManager.isPlayerBusyInGame(player) then return

    combatLogged.remove(player) match
      case Some(task) => task.cancel()
      case None => Message("§c§lCOMBAT!§7 Logging out or teleporting away while in combat will kill you.").send(player)

    combatLogged.put(player, new BukkitRunnable {
      override def run(): Unit =
        if combatLogged.remove (player).isDefined then
        Message ("§a§lCOMBAT OVER!§7 You may now leave without consequences.").send (player)
    }.runTaskLater(NoxetServer.getPlugin, combatLogTimeout))

  def triggerLocationDisband(player: Player): Unit =
    if !isCombatLogged(player) then return

    combatLogged.remove(player)

    val dropAt = player.getLocation
    if dropAt.getWorld == null then return

    val playerInventory = player.getInventory

    val itemsToDrop =
      playerInventory.getItemInOffHand +: // Offhand
        (playerInventory.getContents ++ // Contents
          playerInventory.getArmorContents) // Armor

    for
      itemStack <- itemsToDrop
      if itemStack != null
    do
      dropAt.getWorld.dropItemNaturally(dropAt, itemStack)

  def isCombatLogged(player: Player): Boolean = combatLogged.contains(player)