package org.noxet.noxetserver.menus.inventory

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.noxet.noxetserver.NoxetServer
import org.noxet.noxetserver.commands.social.Friend
import org.noxet.noxetserver.menus.ItemGenerator
import org.noxet.noxetserver.menus.chat.ChatPromptMenu
import org.noxet.noxetserver.messaging.NoteMessage
import org.noxet.noxetserver.playerdata.PlayerDataManager
import org.noxet.noxetserver.realm.RealmManager
import org.noxet.noxetserver.realm.RealmManager.getCurrentRealm
import org.noxet.noxetserver.util.*

import java.util.UUID

class FriendsMenu(player: Player) extends InventoryMenu(
  (PlayerDataManager(player).getListSize(PlayerDataManager.Attribute.FriendList) - 1) / 9 + 2,
  "☻ Friends", false):
  private val friends = Friend.getFriendList(player)

  private val y = getInventory.getSize / 9 - 1
  private val addFriendSlot = InventoryCoordinate(0, y)
  private val viewIncomingSlot = InventoryCoordinate(2, y)
  private val toggleAllowFriendRequestsSlot = InventoryCoordinate(7, y)
  private val toggleFriendTPSlot = InventoryCoordinate(8, y)

  override protected def updateInventory(): Unit =
    for (friendUuidString, i) <- friends.zipWithIndex do
      val friend = PlayerIntel(friendUuidString).get

      val realm = friend.playerOption.map(getCurrentRealm)

      val lastSeen = PlayerDataManager(friend).get(PlayerDataManager.Attribute.LastPlayed).toLong

      setSlotItem(
        ItemGenerator.generatePlayerSkull(
          friend.playerOption.get,
          s"§3${friend.username}",
          Some(List(
            friend.playerOption
              .map("§aOnline" + realm.map(r => s"§e @ §6$r").getOrElse(""))
              .getOrElse(s"§cOffline §7(Last seen ${FancyTimeConverter.deltaSecondsToFancyTime((System.currentTimeMillis / 1000 - lastSeen).toInt)} ago)"),
            friend.playerOption.map("§e→ Double-click to §d§nmessage§e.").getOrElse("§8Messaging unavailable."),
            if friend.playerOption.isDefined &&
              realm.isDefined &&
              realm.doesAllowTeleportationMethods &&
              realm == getCurrentRealm(player)
            then
              "§e→ Shift-click to send §5§nteleportation request§e."
            else
              "§8Teleportation unavailable.",
            "§e→ Press any number on keyboard to §c§nunfriend§e."
          ))
        ),
        InventoryCoordinate.fromSlotIndex(i)
      )

    setSlotItem(
      ItemGenerator.generateItem(
        Material.PAPER,
        "§aAdd Friend",
        List("§7Send a friend request", "§7to someone to remain", "§7in touch.")
      ), addFriendSlot
    )

    val incomingFriendRequests = PlayerDataManager(player).getListSize(PlayerDataManager.Attribute.IncomingFriendRequests)

    setSlotItem(
      ItemGenerator.generateItem(
        Material.WRITABLE_BOOK,
        math.max(incomingFriendRequests, 1),
        s"§dIncoming Friend Requests: §5$incomingFriendRequests",
        List(
          s"§7You have §f$incomingFriendRequests§7 incoming",
          "§7friend requests.",
          "§e→ Click to view requests."
        )
      ), viewIncomingSlot
    )

    val allowFriendRequestsStatus = !PlayerDataManager(player).get(PlayerDataManager.Attribute.DisallowIncomingFriendRequests).toBoolean

    setSlotItem(
      ItemGenerator.generateItem(
        Material.BOOK,
        "§aIncoming Friend Requests",
        List(
          if allowFriendRequestsStatus then "§a§l✔ ENABLED" else "§c§l❌ DISABLED",
          "§7When disabled, players cannot",
          "§7send friend requests to you.",
          "§7Only you can send requests to",
          "§7them (unless they disabled this too).",
          "§7Recommended to disable if you are spammed.",
          s"§e→ Click to ${if allowFriendRequestsStatus then "disable" else "enable"}.")
      ), toggleAllowFriendRequestsSlot
    )

    val friendTeleportStatus = PlayerDataManager(player).get(PlayerDataManager.Attribute.FriendTeleportation).toBoolean

    setSlotItem(
      ItemGenerator.generateItem(
        Material.ENDER_PEARL,
        "§aFriend Teleportation",
        List(
          if friendTeleportStatus then "§a§l✔ ENABLED" else "§c§l❌ DISABLED",
          "§7When enabled, friends who",
          "§7/tpa to you will automatically",
          "§7be teleported without you",
          "§7requiring to accept it.",
          "§c⚠ Only enable if you trust your friends.",
          s"§e→ Click to ${if friendTeleportStatus then "disable" else "enable"}.")
      ), toggleFriendTPSlot
    )

  override protected def onSlotClick(player: Player, coordinate: InventoryCoordinate, clickType: ClickType): Boolean =
    coordinate match
      case addFriendSlot =>
        ChatPromptMenu("name of player to befriend", player, promptResponse =>
          player.performCommand(s"friend add ${promptResponse.getMessage}")
          FriendsMenu(player).openInventory(player)
        )
        return true
      case viewIncomingSlot =>
        IncomingFriendRequestsMenu(player).openInventory(player)
        return true
      case toggleAllowFriendRequestsSlot =>
        player.performCommand("friend toggle-allow-incoming")
        updateInventory()
        return false
      case toggleFriendTPSlot =>
        player.performCommand("friend toggle-friend-tp")
        updateInventory()
        return false
      case c if c.slotIndex > friends.size - 1 =>
        return false
      case _ => ()

    val friend = PlayerIntel(friends(coordinate.slotIndex)).get

    clickType match
      case ClickType.DOUBLE_CLICK =>
        // Message friend
        if friend.isDefined then
          player.performCommand(s"msg ${friend.username}")
          true
        else
          return false
      case ClickType.SHIFT_LEFT => // TPA to friend
        if friend.isEmpty then
          return false

        getCurrentRealm(friend) match
          case Some(friendRealm)
            if friendRealm.allowTeleportationMethods && getCurrentRealm(player).contains(friendRealm) =>
            player.performCommand(s"tpa ${friend.uuid}")
            true
          case _ => false
      case ClickType.NUMBER_KEY =>
        // Remove friend
        ConfirmationMenu(s"End friendship with '${friend}'?",
          Some({
            player.performCommand(s"friend remove ${friend.uuid}")
            FriendsMenu(player).openInventory(player)
          }), Some({
            NoteMessage(s"${friend} is still your friend.").send(player)
            FriendsMenu(player).openInventory(player)
          })).openInventory(player)
        true
      case _ => false