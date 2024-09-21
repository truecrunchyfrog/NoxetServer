package org.noxet.noxetserver.menus.inventory

import org.bukkit.Bukkit
import org.bukkit.entity.HumanEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable
import org.noxet.noxetserver.NoxetServer
import org.noxet.noxetserver.util.{InventoryCoordinate, InventoryCoordinateUtil, QuickRunnable}

import scala.jdk.CollectionConverters.*

abstract class InventoryMenu(rows: Int, title: String, forceOpen: Boolean) extends InventoryHolder, Listener:
    private val inventory = Bukkit.createInventory(this, rows * 9, title)

    NoxetServer.getPlugin.getServer.getPluginManager.registerEvents(this, NoxetServer.getPlugin)

    QuickRunnable(updateInventory()).runTaskLater(NoxetServer.getPlugin, 0)

    protected def updateInventory(): Unit

    /**
     * Listener for when a player clicks a slot.
     * @param player The player who clicked a slot
     * @param coordinate What slot the player clicked
     * @param clickType How the player clicked the slot
     * @return `true` if the menu should be stopped, `false` if it should stay open after the method is called
     */
    protected def onSlotClick(player: Player, coordinate: InventoryCoordinate, clickType: ClickType): Boolean

    protected def setSlotItem(itemStack: ItemStack, s: InventoryCoordinate): Unit =
        assert(s.x >= 0 && s.x < 9 && s.y >= 0 && s.y < inventory.getSize / 9)
        inventory.setItem(s.y * 9 + s.x, itemStack)

    def openInventory(player: Player): Unit = player.openInventory(inventory)

    override def getInventory: Inventory = inventory

    @EventHandler def onInventoryClick(e: InventoryClickEvent): Unit =
        if e.getClickedInventory == inventory then
            if(onSlotClick(
                    e.getWhoClicked.asInstanceOf[Player],
                    InventoryCoordinateUtil.getCoordinateFromSlotIndex(e.getSlot),
                    e.getClick
            ))
                stop()
        else if e.getInventory != inventory then return

        e.setCancelled(true)

    @EventHandler def onInventoryClose(e: InventoryCloseEvent): Unit =
        if e.getInventory != inventory then return
            
        QuickRunnable(() =>
            if forceOpen then
                e.getPlayer.openInventory(e.getInventory)
            else if inventory.getViewers.size == 0 then
                stop()
        ).runTaskLater(NoxetServer.getPlugin, 0)
    

    protected def stop(): Unit =
        HandlerList.unregisterAll(this)

        inventory.getViewers.asScala.foreach(e => QuickRunnable(e.closeInventory()).runTaskLater(NoxetServer.getPlugin, 0))