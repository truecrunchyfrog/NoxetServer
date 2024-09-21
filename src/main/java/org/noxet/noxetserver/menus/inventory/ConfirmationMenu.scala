package org.noxet.noxetserver.menus.inventory

import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.scheduler.BukkitRunnable
import org.noxet.noxetserver.NoxetServer
import org.noxet.noxetserver.menus.ItemGenerator
import org.noxet.noxetserver.menus.inventory.ConfirmationMenu.maxConfirmClicks
import org.noxet.noxetserver.util.{InventoryCoordinate, QuickRunnable, TextBeautifier}

class ConfirmationMenu(question: String, onConfirm: Option[() => Unit], onCancel: Option[() => Unit])
  extends InventoryMenu(4, question, true):
    private var confirmClicksLeft = ConfirmationMenu.maxConfirmClicks

    override protected def updateInventory(): Unit =
        setSlotItem(ItemGenerator.generateItem(
                Material.YELLOW_CONCRETE_POWDER,
                s"§e$question",
                List("§7Choose an option to confirm or cancel.")), 4, 1) // Question.

        setSlotItem(ItemGenerator.generateItem(
                Material.GREEN_CONCRETE_POWDER,
                s"§a${TextBeautifier.beautify("Confirm")}",
                List(s"§e→ Double-click §3§n${if confirmClicksLeft != 1 then confirmClicksLeft + " times" else "once"}§e to confirm.")),
                ConfirmationMenu.confirmButton
        )

        setSlotItem(ItemGenerator.generateItem(
                Material.RED_CONCRETE_POWDER,
                s"§c${TextBeautifier.beautify("Cancel")}",
                List("§e→ Click to cancel.")),
                ConfirmationMenu.cancelButton
        )

    override protected def onSlotClick(player: Player, coordinate: InventoryCoordinate, clickType: ClickType): Boolean =
        coordinate match
          case ConfirmationMenu.confirmButton =>
            if clickType != ClickType.DOUBLE_CLICK then
                return false

            player.playSound(player, Sound.BLOCK_BELL_USE, 1, 1.5f * confirmClicksLeft / maxConfirmClicks + 0.5f)

            confirmClicksLeft -= 1
            if confirmClicksLeft != 0 then
                updateInventory()
                return false

            onConfirm.foreach(r => QuickRunnable(r.run()).runTaskLater(NoxetServer.getPlugin, 5))
            true
          case ConfirmationMenu.cancelButton =>
            onCancel.foreach(r => QuickRunnable(r.run()).runTaskLater(NoxetServer.getPlugin, 5))
            true
          case _ => false

object ConfirmationMenu:
  private val maxConfirmClicks = 3

  private val confirmButton = InventoryCoordinate(1, 2)
  private val cancelButton = InventoryCoordinate(7, 2)