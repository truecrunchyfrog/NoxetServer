package org.noxet.noxetserver.menus.inventory

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.noxet.noxetserver.menus.ItemGenerator
import org.noxet.noxetserver.menus.inventory.SocialMenu.{Blocked, Friends, Party}
import org.noxet.noxetserver.util.InventoryCoordinate

object SocialMenu:
  private val Friends = InventoryCoordinate(2, 1)
  private val Party = InventoryCoordinate(4, 1)
  private val Blocked = InventoryCoordinate(6, 1)

class SocialMenu(private val player: Player) extends InventoryMenu(3, "❤ Social", false):
  override protected def updateInventory(): Unit =
    setSlotItem(
      ItemGenerator.generatePlayerSkull(
        player, "§aFriends", List("§eView friendships.")),
      Friends
    )

    setSlotItem(
      ItemGenerator.generateItem(
        Material.LEAD, "§6Party", List("§eManage your party.")),
      Party
    )

    setSlotItem(
      ItemGenerator.generateItem(Material.BEDROCK, "§cBlocked Players", Collections.singletonList("§eView players you have blocked.")),
      Blocked
    )

  override protected def onSlotClick(player: Player, coordinate: InventoryCoordinate, clickType: ClickType): Boolean =
    coordinate match
      case Friends => FriendsMenu(player).openInventory(player)
      case Party => PartyMenu(player).openInventory(player)
      case Blocked => BlockedPlayersMenu(player).openInventory(player)
      case _ => return false

    true