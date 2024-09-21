package org.noxet.noxetserver.commands.teleportation

import org.bukkit.Location
import org.bukkit.command.{Command, CommandSender, TabExecutor}
import org.bukkit.entity.Player
import org.noxet.noxetserver.Events
import org.noxet.noxetserver.commands.RegisteredCommand
import org.noxet.noxetserver.commands.social.Friend
import org.noxet.noxetserver.commands.teleportation.Home.FriendHomePattern
import org.noxet.noxetserver.menus.chat.ChatPromptMenu
import org.noxet.noxetserver.menus.inventory.HomeNavigationMenu
import org.noxet.noxetserver.messaging.*
import org.noxet.noxetserver.playerdata.PlayerDataManager
import org.noxet.noxetserver.realm.RealmManager
import org.noxet.noxetserver.realm.RealmManager.Realm
import org.noxet.noxetserver.util.{PlayerIntel, TeleportUtil, UsernameStorageManager}

import scala.util.matching.Regex

object Home extends PlayerTabExecutor, RegisteredCommand("home", this):
  val DefaultHomeName = "main"
  val HomeNamePattern: Regex = "[*]?[a-z-]{3,20}".r
  val FriendHomePattern: Regex = "(.+)/(.+)".r

  override def onPlayerCommand(player: Player, command: Command, s: String, strings: Array[String]): Boolean =
    val realm = RealmManager.getCurrentRealm(player)

    if realm.isEmpty then
      ErrorMessage(ErrorMessage.ErrorType.Common, "You must be in a realm to do this.").send(player)
      return true

    if !realm.doesAllowTeleportationMethods then
      ErrorMessage(ErrorMessage.ErrorType.Common, "This realm does not allow you to save homes. You can, however, sleep in beds to save your respawn point. You have to manually transport yourself.").send(player)
      return true

    if strings.length == 0 then
      HomeNavigationMenu(player, realm).openInventory(player)
      return true

    if strings(0).equalsIgnoreCase("friend-tp") then
      if strings.length == 1 then
        ErrorMessage(ErrorMessage.ErrorType.Argument, "Missing argument: friend's home to teleport to.").send(player)
        return false

      val friendTpId = strings(1)

      friendTpId match
        case FriendHomePattern(friendIdentifier, homeNameWithoutStar) =>
          val homeName = '*' + homeNameWithoutStar

          val friend = PlayerIntel(friendIdentifier) match
            case Some(x) => x
            case None =>
              ErrorMessage(ErrorMessage.ErrorType.Common, s"Player '$friendIdentifier' has never been on this server.").send(player)
              return true

          if !Friend.areFriends(player, friend) then
            ErrorMessage(ErrorMessage.ErrorType.Common, s"You are not friends with $friend, and cannot teleport to their friend homes.").send(player)
            return true

          val friendHomes = getRealmHomes(friend, realm)

          if !friendHomes.contains(homeName) then
            ErrorMessage(ErrorMessage.ErrorType.Common, s"$friend does not friend share a home by that name.").send(player)
            return true

          var homeLocation = friendHomes.get(homeName)

          if !TeleportUtil.isLocationTeleportSafe(homeLocation) then
            val arg = strings.length > 2
            val safe = arg && strings(2).equalsIgnoreCase("safe")
            val force = arg && strings(2).equalsIgnoreCase("force")

            if safe then
              Message("§aFinding a safe location nearby friend's home...").send(player)
              homeLocation = TeleportUtil.getSafeTeleportLocation(homeLocation)
              if homeLocation.isEmpty then
                ErrorMessage(ErrorMessage.ErrorType.Common, "We could not find a safe location nearby that home.").send(player)
                return true
            else if force then
              Message("§aForcing teleport to friend's home...").send(player)
            else
              WarningMessage("This friend home is suspiciously located. It may not be safe to teleport there.").addButton(
                "Teleport safely nearby",
                ChatColor.GREEN,
                "Let us find a safe location for you to teleport nearby this home",
                s"home friend-tp $friendTpId safe"
              ).addButton(
                "Teleport anyway",
                ChatColor.RED,
                "Do this at your own risk",
                s"home friend-tp $friendTpId force"
              ).send(player)

              return true

          if player.teleport(homeLocation) then
            Events.setTemporaryInvulnerability(player)
            Message(s"§3You teleported to $friendTpId.").send(player)

          return true
        case _ =>
          ErrorMessage(ErrorMessage.ErrorType.Argument, "Incorrect syntax. The syntax is: 'friend-name/home-name'.").send(player)
          return true

    val homes = getHomes(player)
    val realmHomes = getRealmHomes(player, realm)

    val homeName = if strings.length >= 2 then strings(1).toLowerCase else defaultHomeName

    if !isHomeNameOk(homeName) then
      getHomeNameNotOkMessage.send(player)
      return true

    strings(0).toLowerCase match
      case "tp" =>
        var homeLocation = realmHomes.get(homeName)

        if homeLocation.isEmpty then
          ErrorMessage(ErrorMessage.ErrorType.Common,
            s"You do not have a home saved by the name '$homeName'.")
            .addButton("List homes", ChatColor.YELLOW, "See your saved homes", "home list").send(player)
          return true

        if !TeleportUtil.isLocationTeleportSafe(homeLocation) then
          val arg = strings.length > 2
          val safe = arg && strings(2).equalsIgnoreCase("safe")
          val force = arg && strings(2).equalsIgnoreCase("force")

          if safe then
            Message("§aFinding a safe location nearby...").send(player)
            homeLocation = TeleportUtil.getSafeTeleportLocation(homeLocation)
            if homeLocation.isEmpty then
              ErrorMessage(ErrorMessage.ErrorType.Common, "We could not find a safe location nearby that home.").send(player)
              return true
          else if force then
            Message("§aForcing teleport to home...").send(player)
          else
            WarningMessage("This home may not be safe to teleport to.").addButton(
              "Teleport safely nearby",
              ChatColor.GREEN,
              "Let us find a safe location for you to teleport nearby your home",
              s"home tp $homeName safe"
            ).addButton(
              "Teleport anyway",
              ChatColor.RED,
              "Do this at your own risk",
              s"home tp $homeName force"
            ).send(player)

            return true

        if player.teleport(homeLocation) then
          Events.setTemporaryInvulnerability(player)
          Message("§3Welcome home!").send(player)
        else
          ErrorMessage(ErrorMessage.ErrorType.Common, "Sorry, you could not be teleported to your home. Please report this.").send(player)
      case "set" =>
        if homeName == "?" then
          ChatPromptMenu("home name", player, promptResponse =>
            player.performCommand(s"home set ${promptResponse.getMessage}"))
          return true

        if realmHomes.contains(homeName) && !(strings.length >= 3 && strings[2].equalsIgnoreCase("overwrite")) then
          ErrorMessage(ErrorMessage.ErrorType.Common, s"You have already saved a home as '$homeName'.").addButton("Overwrite", ChatColor.RED, "Overwrite your existing home by this name", s"home set $homeName overwrite").send(player)
          return true

        realmHomes.filter((name, location) =>
          name != homeName &&
            player.getWorld == location.getWorld &&
            player.getLocation.distance(location) < 50
        ).foreach(NoteMessage(s"Your home '$name' is quite near this location.").send(player))

        val overwrote = realmHomes.put(homeName, player.getLocation) != null // TODO fix

        if realmHomes.size > 50 then
          ErrorMessage(ErrorMessage.ErrorType.Common, "You have reached your home limit. Max 50 homes are allowed per player. Consider deleting other homes before making another one.").send(player)
          return true

        homes.put(realm.name, realmHomes)

        PlayerDataManager(player).set(PlayerDataManager.Attribute.Homes, homes).save()

        SuccessMessage(s"Home '$homeName' has been saved.").send(player)

        if overwrote then
          NoteMessage("Old home location by same name was overwritten.").send(player)

        if isHomeFriendShared(homeName) then
          WarningMessage("The home you just created is friend shared! Any friend of yours can use that home.").send(player)
      case "remove" =>
        if !realmHomes.containsKey(homeName) then
          ErrorMessage(ErrorMessage.ErrorType.Common, s"You do not have a home called '$homeName'.").send(player)
          return true

        realmHomes.remove(homeName)
        homes.put(realm.name, realmHomes)

        PlayerDataManager(player).set(PlayerDataManager.Attribute.Homes, homes).save()

        SuccessMessage(s"Home '$homeName' has been removed.")
          .send(player)

        if isHomeFriendShared(homeName) then
          NoteMessage("That home was friend shared. Your friends can no longer use it either.")
            .send(player)
      case "list" =>
        Message(s"§eHomes: ${realmHomes.size}").send(player)

        if realmHomes.isEmpty then
          Message("You don't have any home yet!")
            .addButton("Add home here", ChatColor.GREEN, "Set your default home to here", "home set")
            .send(player)
          return true

        realmHomes.foreach((name, _) =>
          Message(s"└§a§lHOME §6$name")
            .addButton("Go", ChatColor.GREEN, s"Teleport to '$name'", s"home tp $name")
            .send(player)
        )
      case "rename" =>
        if !realmHomes.containsKey(homeName) then
          ErrorMessage(ErrorMessage.ErrorType.Common, s"You do not have a home called '$homeName'.").send(player)
          return true

        if strings.length < 3 then
          ErrorMessage(ErrorMessage.ErrorType.Argument, "Missing argument: what the home should be renamed to.").send(player)
          return true

        val newName = strings(2)

        if !isHomeNameOk(newName) then
          getHomeNameNotOkMessage.send(player)
          return true

        if realmHomes.contains(newName) then
          ErrorMessage(ErrorMessage.ErrorType.Common, s"You already have a home called '$newName'.").send(player)
          return true

        realmHomes.put(newName, realmHomes.get(homeName))
        realmHomes.remove(homeName)
        homes.put(realm.name, realmHomes)

        PlayerDataManager(player).set(PlayerDataManager.Attribute.Homes, homes).save()

        SuccessMessage(s"Home '$homeName' has been renamed to '$newName'.").send(player)

        if isHomeFriendShared(homeName) && !isHomeFriendShared(newName) then
          WarningMessage("You disabled friend sharing for that home. Your friends can no longer use that home of yours.").send(player)

        if !isHomeFriendShared(homeName) && isHomeFriendShared(newName) then
          WarningMessage("You enabled friend sharing for that home. Your friends can now use that home to teleport to.").send(player)
      case _ =>
        ErrorMessage(ErrorMessage.ErrorType.Argument, s"Invalid home method: '${strings(0)}'.").send(player)
        return false

    true

  override def onPlayerTabComplete(player: Player, command: Command, s: String, strings: Array[String]): List[String] =
    strings match
      case Array(_) =>
        List("tp", "set", "remove", "list", "rename", "friend-tp")
      case Array(action, _) =>
        (action.toLowerCase, RealmManager.getCurrentRealm(player)) match
          case ("tp" | "remove" | "rename", Some(realm)) =>
            getRealmHomes(player, realm).values
          case ("friend-tp", Some(realm)) => getFriendsHomes(player, realm)
          case _ => List()
      case _ => List()

  def isHomeNameOk(name: String): Boolean = HomeNamePattern.pattern.matcher(name).matches

  def getHomeNameNotOkMessage: ErrorMessage =
    ErrorMessage(ErrorMessage.ErrorType.Argument,
      "Home name can only consist of 3-20 characters: alphanumeric (a-z, 0-9) and dashes (\"-\"). Optionally, a star (\"*\") can be appended to indicate a friend home.")

  def getHomes(uuid: UUID): Map[String, Map[String, Location]] =
    PlayerDataManager(uuid).get(PlayerDataManager.Attribute.HOMES).asInstanceOf[Map[String, Map[String, Location]]]

  def getRealmHomes(uuid: UUID, realm: Realm): Map[String, Location] =
    getHomes(uuid).getOrDefault(realm.name, HashMap())

  /**
   * Get a string in the friend home notation: `friend_name/home-name`
   *
   * @param friendName the username of the friend who owns the home.
   * @param homeName   the name of the home, including the friend home star `*`.
   * @return a friend home denoted string.
   * @example [[getFriendHomeName("joe", "*home")]] returns `joe/home`.
   */
  def getFriendHomeName(friendName: String, homeName: String): String =
    friendName + '/' + homeName.substring(1)

  /**
   * Retrieves a list of all friend homes belonging to all friends.
   *
   * @param uuid  the UUID of the player whose friends' homes are returned.
   * @param realm the realm in which the homes should be listed.
   * @return a list of homes and their corresponding owners.
   */
  def getFriendsHomes(uuid: UUID, realm: Realm): List[(PlayerIntel, (String, Location))] =
    Friend.getFriendList(uuid)
      .map(PlayerIntel).flatMap
      .flatMap(
        friend => getRealmHomes(friend.uuid, realm)
          .toList
          .filter(isHomeFriendShared(_._1))
          .map((friend, _)))

  /**
   * Same as [[getFriendsHomes]], but formatted into readable strings with [[getFriendHomeName]].
   *
   * @param uuid  the UUID of the player whose friends' homes are returned.
   * @param realm the realm in which the homes should be listed.
   * @return a list of strings formatted "friend-name/home-name", for each friend's friend home.
   * @note friend home names actually start with a star `*`, but they are excluded in this list.
   */
  def getFriendsHomesFormatted(uuid: UUID, realm: Realm): List[String] =
    getFriendsHomes(uuid, realm).map(getFriendHomeName(_._1.username, _._2._1))

  def isHomeFriendShared(homeName: String): Boolean = homeName.startsWith("*")