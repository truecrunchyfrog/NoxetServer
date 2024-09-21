package org.noxet.noxetserver.menus.inventory

import org.bukkit.block.banner.{Pattern, PatternType}
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.meta.BannerMeta
import org.bukkit.inventory.{ItemFlag, ItemStack}
import org.bukkit.scheduler.{BukkitRunnable, BukkitTask}
import org.bukkit.{DyeColor, Location, Material, World}
import org.noxet.noxetserver.NoxetServer
import org.noxet.noxetserver.commands.social.Friend
import org.noxet.noxetserver.commands.teleportation.Home
import org.noxet.noxetserver.menus.ItemGenerator
import org.noxet.noxetserver.menus.chat.ChatPromptMenu
import org.noxet.noxetserver.playerdata.PlayerDataManager
import org.noxet.noxetserver.realm.RealmManager.Realm
import org.noxet.noxetserver.util.{InventoryCoordinate, Sample, UsernameStorageManager}

import java.text.DecimalFormat
import scala.language.postfixOps

class HomeNavigationMenu(player: Player, realm: Realm) extends InventoryMenu(
  (Home.getRealmHomes(player, realm).size - 1) / 9 + 2 +
    (if PlayerDataManager(player).get(PlayerDataManager.Attribute.SHOW_FRIEND_HOMES).toBoolean then (Home.getFriendsHomes(player, realm).size - 1) / 9 else 0),
  s"⚑ ${realm.getDisplayName} Homes",
  false):
  private val homes = Home.getRealmHomes(player, realm)
  private val toggleFriendsHomesSlot = InventoryCoordinate(0, getInventory.getSize / 9 - 1)
  private val showFriendsHomes = PlayerDataManager(player).get(PlayerDataManager.Attribute.SHOW_FRIEND_HOMES).toBoolean
  private val friendsHomes = if showFriendsHomes then Home.getFriendsHomes(player, realm) else List()

  private var friendSkullScrollIndex = 0

  val friends: List[String] = Friend.getFriendList(player.getUniqueId)

  private val toggleFriendsHomesSlotUpdateTimer = QuickRunnable(() =>
    val playerToShow =
      if friends.nonEmpty then
        friendSkullScrollIndex = (friendSkullScrollIndex + 1) % friends.size
        NoxetServer.getPlugin.getServer
          .getOfflinePlayer(UsernameStorageManager.getUuidFromUsernameOrUuid(
            friends.get(friendSkullScrollIndex)))
      else
        player

    setSlotItem(ItemGenerator.generatePlayerSkull(
      playerToShow,
      "§aToggle Friends' Homes",
      List(
        if showFriendsHomes then "§a§l✔ SHOWN" else "§c§l❌ HIDDEN",
        "§7When enabled, your friends' shared",
        "§7homes will be shown in this list.",
        s"§e→ Click to ${if showFriendsHomes then "hide" else "show"}."
      )
    ), toggleFriendsHomesSlot)
  ).runTaskTimer(NoxetServer.getPlugin, 0, 40)

  override protected def updateInventory(): Unit =
    for ((name, loc), i) <- homes.zipWithIndex do
      val blocksAway =
        if loc.getWorld == player.getWorld then
          Some(player.getLocation.distance(loc).toInt)
        else
          None

      val locationNote = "§5" + (loc.getWorld.getEnvironment match
        case NORMAL => "Overworld"
        case NETHER => "Nether"
        case THE_END => "The End"
        case _ => "???") +
        blocksAway
          .map(s" §e${DecimalFormat("#,###").format(blocksAway)} blocks away.")
          .getOrElse("")

      val isMainHome = name == Home.defaultHomeName

      val lore = List(
        locationNote,
        s"§8X §7${loc.getX.toInt}",
        s"§8Y §7${loc.getY.toInt}",
        s"§8Z §7${loc.getZ.toInt}",
        "§e→ Double-click to §5§nteleport§e.",
        "§e→ Right-click to §b§nrename§e.",
        "§e→ Press any number on keyboard to §c§nremove§e this home.",
        s"§e→ Shift-right-click to §d§n${if !Home.isHomeFriendShared(name) then "enable" else "disable"} friend sharing§e."
      )

      val homeItem = ItemGenerator.generateItem(
        if !isMainHome then Material.WHITE_BANNER else Material.RED_BED,
        if !Home.isHomeFriendShared(name) then
          (if !isMainHome then "§a" else "§5◆ §3") + name
        else
          "§5☮ §a" + name.substring(1),
        lore
      )

      if !isMainHome then
        generateBannerPatterns(homeItem, loc.hashCode)

      setSlotItem(homeItem, InventoryCoordinate.fromSlotIndex(i))

    for ((friend, (homeName, _)), i) <- friendsHomes.zipWithIndex do
      setSlotItem(
        ItemGenerator.generatePlayerSkull(friend.playerOption.get,
          "§3" + Home.getFriendHomeName(friend.username, homeName),
          List(
            s"§7This home is shared by §b${friend.username}§7.",
            "§e→ Double-click to §5§nteleport§e."
          )), InventoryCoordinate.fromSlotIndex(i + homes.size))

  override protected def onSlotClick(player: Player, coordinate: InventoryCoordinate, clickType: ClickType): Boolean = {
    if coordinate == toggleFriendsHomesSlot then
      PlayerDataManager(player).toggleBoolean(PlayerDataManager.Attribute.SHOW_FRIEND_HOMES).save()
      HomeNavigationMenu(player, realm).openInventory(player)
      return false

    homes.zipWithIndex // add index to...
      .find(InventoryCoordinate.fromSlotIndex(_._2) == coordinate).map(_._1) match // ... find clicked home & remove index
      case Some((name, loc)) => return clickType match
        case ClickType.DOUBLE_CLICK =>
          player.performCommand(s"home tp $name")
          true
        case ClickType.NUMBER_KEY =>
          ConfirmationMenu(s"Delete home '$name'?", () => {
            player.performCommand(s"home remove $name")
            HomeNavigationMenu(player, realm).openInventory(player)
          }, HomeNavigationMenu(player, realm).openInventory(player))
            .openInventory(player)
          true
        case ClickType.RIGHT =>
          ChatPromptMenu(
            s"new name for home '$name'",
            player,
            response =>
              player.performCommand(s"home rename $name ${response.getMessage}")
              HomeNavigationMenu(player, realm).openInventory(player)
          )
          true
        case ClickType.SHIFT_RIGHT =>
          val isAlreadyFriendShared = Home.isHomeFriendShared(name)
          ConfirmationMenu(s"${if isAlreadyFriendShared then "Disable" else "Enable"} friend sharing for home '$name'?", () =>
            player.performCommand(
              s"home rename $name " + (if isAlreadyFriendShared then name.substring(1) else s"*$name")
            )
            HomeNavigationMenu(player, realm).openInventory(player)
            , () => HomeNavigationMenu(player, realm).openInventory(player)).openInventory(player)
          true
        case _ => false
      case None => ()

    friendsHomes.zipWithIndex
      .find(InventoryCoordinate.fromSlotIndex(_._2 + homes.size) == coordinate).map(_._1) match
      case Some((player, (name, loc))) => clickType match
        case ClickType.DOUBLE_CLICK =>
          player.performCommand(s"home friend-tp ${Home.getFriendHomeName(player.username, name)}")
          true
        case _ => false
      case None => false

  private def generateBannerPatterns(banner: ItemStack, seed: Int): Unit =
    given Random(seed)

    val patterns = for _ <- 0 until 10 yield
      // Append random patterns to the banner.
      // (based on location seed)
      Pattern(DyeColor.values.sampleOne.get, PatternType.values.sampleOne)

    val bannerMeta = banner.getItemMeta.asInstanceOf[BannerMeta]
    assert(bannerMeta != null)

    bannerMeta.setPatterns(patterns)
    bannerMeta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS) // Hide banner pattern lore.

    banner.setItemMeta(bannerMeta)

  override protected def stop(): Unit =
    super.stop()
    toggleFriendsHomesSlotUpdateTimer.cancel()