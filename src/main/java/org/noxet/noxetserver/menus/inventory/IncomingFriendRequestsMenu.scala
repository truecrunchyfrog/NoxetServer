package org.noxet.noxetserver.menus.inventory

import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.noxet.noxetserver.NoxetServer
import org.noxet.noxetserver.menus.ItemGenerator
import org.noxet.noxetserver.playerdata.PlayerDataManager
import org.noxet.noxetserver.util.{InventoryCoordinate, InventoryCoordinateUtil, PlayerIntel, UsernameStorageManager}

import java.util.UUID

class IncomingFriendRequestsMenu(player: Player) extends InventoryMenu(
  (PlayerDataManager(player).getListSize(PlayerDataManager.Attribute.BlockedPlayers) - 1) / 9 + 1,
  "✉ Incoming Friend Requests",
  false):
  private val incomingPlayers: List[PlayerIntel] =
    PlayerDataManager(player)
      .get(PlayerDataManager.Attribute.IncomingFriendRequests).asInstanceOf[List[String]]
      .flatMap(PlayerIntel)

  override protected def updateInventory(): Unit =
    incomingPlayers
      .zipWithIndex
      .foreach((incomingPlayer, i) =>
        setSlotItem(
          ItemGenerator.generatePlayerSkull(
            NoxetServer.getPlugin.getServer.getOfflinePlayer(incomingPlayer),
            s"§e$incomingPlayer",
            List(
              "§7This player wants to befriend you.",
              "§e→ Double-click to §a§naccept§e.",
              "§e→ Shift-click to §c§ndeny§e.",
              "§e→ Press any number on keyboard to §8§nblock§e this player."
            )
          ),
          InventoryCoordinate.fromSlotIndex(i)
        )
      )

  override protected def onSlotClick(player: Player, coordinate: InventoryCoordinate, clickType: ClickType): Boolean =
    incomingPlayers.lift(coordinate.slotIndex) match
      case Some(clickedPlayer) =>
        clickType match
          case ClickType.DOUBLE_CLICK =>
            player.performCommand(s"friend add $clickedPlayer")
          case ClickType.SHIFT_LEFT =>
            player.performCommand(s"friend deny $clickedPlayer")
          case ClickType.NUMBER_KEY =>
            ConfirmationMenu(s"Block '$clickedPlayer'?", () =>
              player.performCommand(s"block $clickedPlayer")
              IncomingFriendRequestsMenu(player).openInventory(player)
              , () => IncomingFriendRequestsMenu(player).openInventory(player)).openInventory(player)
            return true

        IncomingFriendRequestsMenu(player).openInventory(player)
        true
      case None => false