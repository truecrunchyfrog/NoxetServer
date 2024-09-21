package org.noxet.noxetserver.menus.inventory

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.noxet.noxetserver.NoxetServer
import org.noxet.noxetserver.menus.ItemGenerator
import org.noxet.noxetserver.menus.chat.ChatPromptMenu
import org.noxet.noxetserver.playerdata.PlayerDataManager
import org.noxet.noxetserver.util.{InventoryCoordinate, InventoryCoordinateUtil, UsernameStorageManager}

import java.util.{Arrays, UUID}

class BlockedPlayersMenu(player: Player) extends InventoryMenu(
  (PlayerDataManager(player).getListSize(PlayerDataManager.Attribute.BlockedPlayers) - 1) / 9 + 2,
  "❌ Blocked Players",
  false):

  private val blockedPlayers = PlayerDataManager(player.getUniqueId).get(PlayerDataManager.Attribute.BlockedPlayers).asInstance[List[String]]
  private val blockNewPlayerSlot = InventoryCoordinateUtil.getCoordinateFromXY(0, getInventory.getSize / 9 - 1)

  override protected def updateInventory(): Unit =
    for blockedUuidString <- blockedPlayers do
      val blockedUuid = UsernameStorageManager().getUuidFromUsernameOrUuid(blockedUuidString)
      val blockedName = UsernameStorageManager.getCasedUsernameFromUuid(blockedUuid)

      setSlotItem(
        ItemGenerator.generatePlayerSkull(
          NoxetServer.getPlugin.getServer.getOfflinePlayer(blockedUuid),
          s"§c${blockedName.getOrElse(blockedUuidString)}",
          Some(List(
            s"§7UUID: $blockedUuidString",
            "§e→ Double-click to §a§npardon§e."
          ))
        ),
        InventoryCoordinateUtil.getCoordinateFromSlotIndex(blockedPlayers.indexOf(blockedUuidString))
      )

    setSlotItem(
      ItemGenerator.generateItem(
        Material.PAPER,
        "§aBlock Player",
        Some(List(
          "§7Block a player to",
          "§7prevent them from sending",
          "§7messages and friend/TPA requests."
        ))
      ), blockNewPlayerSlot
    )

  override protected def onSlotClick(player: Player, coordinate: InventoryCoordinate, clickType: ClickType): Boolean =
    if coordinate.isAt(blockNewPlayerSlot) then
      ChatPromptMenu("player to block", player, promptResponse => {
        player.performCommand(s"block ${promptResponse.getMessage}")
        BlockedPlayersMenu(player).openInventory(player)
      })
      return true

    if clickType != ClickType.DOUBLE_CLICK || coordinate.slotIndex > blockedPlayers.size - 1 then
      return false

    val clickedBlockedUuid = blockedPlayers.get(coordinate.slotIndex)

    player.performCommand(s"unblock $clickedBlockedUuid")

    BlockedPlayersMenu(player).openInventory(player)

    true