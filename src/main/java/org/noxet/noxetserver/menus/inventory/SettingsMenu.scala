package org.noxet.noxetserver.menus.inventory

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.noxet.noxetserver.menus.ItemGenerator
import org.noxet.noxetserver.menus.chat.ChatPromptMenu
import org.noxet.noxetserver.menus.inventory.SettingsMenu.EraseUserData
import org.noxet.noxetserver.messaging.ErrorMessage
import org.noxet.noxetserver.util.{InventoryCoordinate, PlayerDataEraser}

object SettingsMenu:
  private val EraseUserData = InventoryCoordinate(8, 2)

class SettingsMenu extends InventoryMenu(3, "Settings", false):
  override protected def updateInventory(): Unit =
    setSlotItem(
      ItemGenerator.generateItem(
        Material.SKELETON_SKULL,
        "§4Erase your data",
        List(
          "§7Any data saved on your",
          "§7account will be deleted.",
          "§6§lIRREVERSIBLE!",
          "§eConsider carefully before",
          "§eerasing your data."
        )
      ), EraseUserData)

  override protected def onSlotClick(player: Player, coordinate: InventoryCoordinate, clickType: ClickType): Boolean =
    coordinate match
      case EraseUserData =>
        val confirmText = s"I ${player.getName} HEREBY RESIGN"
        ChatPromptMenu(
          s"the message: \"$confirmText\", to have your account data on Noxet deleted in 3 days (cancel any time)",
          player, promptResponse => {
            if promptResponse.getMessage.equalsIgnoreCase(confirmText) then
              PlayerDataEraser.planDataErasure(player)
              player.kickPlayer(
                s"§cGoodbye, ${player.getName}...\n\nYou have requested erasure of your data.\nEverything linked to your account (except e.g. in-game signs, world and chest items) will be removed in 3 days.\nLog in within this time to abort the removal.")
              return

            ErrorMessage(ErrorMessage.ErrorType.Common, "Wrong message entered. Nothing happened.").send(player)
          })
        true
      case _ => false