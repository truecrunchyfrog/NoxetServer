package org.noxet.noxetserver.commands.social

import org.bukkit.command.{Command, CommandSender, TabExecutor}
import org.bukkit.entity.Player
import org.noxet.noxetserver.NoxetServer
import org.noxet.noxetserver.commands.RegisteredCommand
import org.noxet.noxetserver.menus.inventory.FriendsMenu
import org.noxet.noxetserver.messaging.{ErrorMessage, Message, NoteMessage, SuccessMessage}
import org.noxet.noxetserver.playerdata.PlayerDataManager
import org.noxet.noxetserver.playerdata.PlayerDataManager.Attribute.*
import org.noxet.noxetserver.util.{PlayerIntel, UsernameStorageManager}

import java.util.UUID
import scala.collection.immutable.{AbstractSeq, LinearSeq}

object Friend extends PlayerTabExecutor, RegisteredCommand("friend", this):
  val MaxOutgoingRequests = 5
  val MaxIncomingRequests = 20
  val MaxFriends = 30

  override def onPlayerCommand(player: Player, command: Command, s: String, strings: Array[String]): Boolean =
    strings match
      case Array() =>
        FriendsMenu(player).openInventory(player)
        true
      case Array("add") =>
        ErrorMessage(ErrorMessage.ErrorType.Argument, "Missing argument: player to befriend.").send(player)
        true
      case Array("add", playerIdentifier, args*) =>
        val befriend = PlayerFragments(playerIdentifier) match
          case Some(x) => x
          case None =>
            ErrorMessage(ErrorMessage.ErrorType.Common, "That player is not registered.").send(player)
            return true

        if player.getUniqueId == befriend.uuid then
          ErrorMessage(ErrorMessage.ErrorType.Common, "You cannot befriend yourself.").send(player)
          return true

        if areFriends(player, befriend) then
          ErrorMessage(ErrorMessage.ErrorType.Common, "You are already friends!").send(player)
          return true

        args.headOption match
          case Some("!") if player.isOp =>
            acceptRequest(befriend, player)
            SuccessMessage(s"Forced friendships are the best! You are now friends with $befriend.").send(player)
            return true
          case _ => ()

        if hasReceivedFriendRequestFrom(befriend, player) then
          ErrorMessage(ErrorMessage.ErrorType.Common, "You have already sent a friend request to them. Please wait for them to respond.").send(player)
          return true

        if getFriendList(player).size >= maxFriends then
          ErrorMessage(ErrorMessage.ErrorType.Common, "You have reached your friend limit.").send(player)
          return true

        if PlayerDataManager(player).doesContain(BlockedPlayers, befriend.uuid.toString) then
          ErrorMessage(ErrorMessage.ErrorType.Common, "You have blocked this player, and cannot send friend requests to them.").send(player)
          return true

        if PlayerDataManager(befriend).doesContain(BlockedPlayers, player.getUniqueId.toString) ||
          PlayerDataManager(befriend).get(DisallowIncomingFriendRequests).toBoolean
        then
          ErrorMessage(ErrorMessage.ErrorType.Common, "You may not send friend requests to this player.").send(player)
          return true

        if getFriendList(befriend).size >= maxFriends then
          ErrorMessage(ErrorMessage.ErrorType.Common, s"$befriend has reached their friend limit!").send(player)
          return true

        if hasReceivedFriendRequestFrom(player, befriend) then
          acceptRequest(player, befriend)

          SuccessMessage(s"You are now friends with $befriend!").send(player)
          befriend.playerOption.foreach(SuccessMessage(s"${player.getName} accepted your friend request.").send(_))

          return true

        if getOutgoingFriendRequests(player).size >= MaxOutgoingRequests then
          ErrorMessage(ErrorMessage.ErrorType.Common, "You have reached your limit on max amount of outgoing friend requests.").send(player)
          return true

        if getIncomingFriendRequests(befriend).size >= MaxIncomingRequests then
          ErrorMessage(ErrorMessage.ErrorType.Common, s"$befriend has enough friend requests to take care of, let them be!").send(player)
          return true

        sendFriendRequest(player, befriend)

        Message(s"§eYou sent a friend request to $befriend.").send(player)

        befriend.playerOption.foreach(
          Message(s"§e${player.getName} sent you a friend request!")
            .addButton("Accept", ChatColor.GREEN, "Become friends!", s"friend add ${player.getName}")
            .addButton("Deny", ChatColor.RED, "Don't become friends", s"friend deny ${player.getName}")
            .send)

        true
      case Array("remove") =>
        ErrorMessage(ErrorMessage.ErrorType.Argument, "Missing argument: player to unfriend.").send(player)
        true
      case Array("remove", playerIdentifier, args*) =>
        val unfriend = PlayerFragments(playerIdentifier) match
          case Some(x) => x
          case None =>
            ErrorMessage(ErrorMessage.ErrorType.Common, "That player is not registered.").send(player)
            return true

        if !areFriends(player, unfriend) then
          ErrorMessage(ErrorMessage.ErrorType.Common, "You are not friends!").send(player)
          return true

        removeFriend(player, unfriend)

        SuccessMessage(s"$unfriend is no longer your friend.").send(player)
        unfriend.playerOption.foreach(Message(s"§c${player.getName} removed you as their friend.").send(_))

        true
      case Array("deny") =>
        ErrorMessage(ErrorMessage.ErrorType.Argument, "Missing argument: player to deny.").send(player)
        true
      case Array("deny", playerIdentifier, args*) =>
        val denyPlayer = PlayerFragments(playerIdentifier) match
          case Some(x) => x
          case None =>
            ErrorMessage(ErrorMessage.ErrorType.Common, "That player is not registered.").send(player)
            return true

        if !hasReceivedFriendRequestFrom(player, denyPlayer) then
          ErrorMessage(ErrorMessage.ErrorType.Common, "They have not sent you a friend request!").send(player)
          return true

        denyRequest(player, denyPlayer)

        SuccessMessage(s"Denied the friend request from $denyPlayer.").send(player)
        denyPlayer.playerOption.foreach(Message(s"§c${player.getName} denied your friend request.").send)

        args.headOption match
          case Some("!") =>
            player.performCommand(s"block ${denyPlayer.username}")
          case _ => ()

        true
      case Array("cancel") =>
        ErrorMessage(ErrorMessage.ErrorType.Argument, "Missing argument: player to cancel request to.").send(player)
        true
      case Array("cancel", playerIdentifier, args*) =>
        val cancelTo = PlayerFragments(playerIdentifier) match
          case Some(x) => x
          case None =>
            ErrorMessage(ErrorMessage.ErrorType.Common, "That player is not registered.").send(player)
            return true

        if !hasReceivedFriendRequestFrom(cancelTo, player) then
          ErrorMessage(ErrorMessage.ErrorType.Common, "You have not sent them a friend request!").send(player)
          return true

        denyRequest(cancelTo, player)

        SuccessMessage(s"Canceled the friend request to $cancelTo.").send(player)

        true
      case Array("list", _*) =>
        val friendUuids = getFriendList(player.getUniqueId)
        Message(s"§eFriends: ${friendUuids.size}").send(player)
        friendUuids.map(PlayerIntel).foreach(friend => Message(s"└§a§lFRIEND §2$friend").send(player))
        true
      case Array("incoming", _*) =>
        val incomingUuids = getIncomingFriendRequests(player.getUniqueId)
        Message(s"§eIncoming friend requests: ${incomingUuids.size}").send(player)
        incomingUuids.map(PlayerIntel).foreach(incomingFriend =>
          Message(s"└§6§lINCOMING §a${incomingFriend.username}")
            .addButton("Accept", ChatColor.GREEN, "Become friends!", s"friend add $incomingFriend")
            .addButton("Deny", ChatColor.RED, "Don't become friends", s"friend deny $incomingFriend")
            .addButton("Deny and block", ChatColor.GRAY, s"Deny this request and block $incomingFriend", s"friend deny $incomingFriend !")
            .send(player))
        true
      case Array("outgoing", _*) =>
        val outgoingUuids = getOutgoingFriendRequests(player)
        Message(s"§eOutgoing friend requests: ${outgoingUuids.size}").send(player)
        outgoingUuids.map(PlayerIntel).foreach(outgoingFriend =>
          Message(s"└§8§lOUTGOING §7$outgoingFriend")
            .addButton("Cancel", ChatColor.RED, "Regret wanting to become friends?", s"friend cancel $outgoingFriend")
            .send(player))
        true
      case Array("toggle-allow-incoming", _*) =>
        val playerDataManager = PlayerDataManager(player)
        val setTo = !playerDataManager.get(DisallowIncomingFriendRequests).toBoolean
        playerDataManager.set(DisallowIncomingFriendRequests, setTo).save()
        SuccessMessage(
          (if setTo then "Disabled" else "Enabled") +
            " incoming friend requests. Other players can " +
            (if setTo then "no longer" else "now") +
            " send friend requests to you.").send(player)
        if setTo && playerDataManager.getListSize(IncomingFriendRequests) != 0 then
          NoteMessage("You have incoming friend requests already pending. If you want, you can cancel those manually.")
            .addButton("Manage", ChatColor.YELLOW, "Manage incoming friend requests", "friend incoming")
            .send(player)
        true
      case Array("toggle-friend-tp", _*) =>
        val playerDataManager = PlayerDataManager(player)
        val setTo = !playerDataManager.get(FriendTeleportation).toBoolean
        playerDataManager.set(FriendTeleportation, setTo).save()
        SuccessMessage(
          s"${if setTo then "Enabled" else "Disabled"} friend teleportation." +
            s"Your friends can ${if setTo then "now" else "no longer"} /tpa to you without you accepting.").send(player)
        true
      case Array(action, _*) =>
        ErrorMessage(ErrorMessage.ErrorType.Argument, s"Sorry, '$action' is not a valid friend command.").send(player)
        true

  override def onPlayerTabComplete(player: Player, command: Command, s: String, strings: Array[String]): List[String] =
    strings match
      case Array(_) =>
        List("add", "remove", "deny", "list", "incoming", "outgoing", "toggle-allow-incoming", "toggle-friend-tp")
      case Array(action, arg) =>
        action match
          case "add" =>
            Option(NoxetServer.getPlugin.getServer.getPlayer(arg))
              .map(_.getName)
              .toList
          case "deny" =>
            getIncomingFriendRequests(player)
              .map(PlayerIntel)
              .map(_.username)
          case "remove" =>
            getFriendList(player)
              .map(PlayerIntel)
              .map(_.username)
          case "cancel" =>
            getOutgoingFriendRequests(player)
              .map(PlayerIntel)
              .map(_.username)
      case _ => List()

  def sendFriendRequest(from: UUID, to: UUID): Unit =
    PlayerDataManager(from).addToStringList(OutgoingFriendRequests, to.toString).save()
    PlayerDataManager(to).addToStringList(IncomingFriendRequests, from.toString).save()

  def areFriends(player1: UUID, player2: UUID): Boolean =
    getFriendList(player1).contains(player2.toString) &&
      getFriendList(player2).contains(player1.toString)

  def getFriendList(uuid: UUID): List[String] =
    PlayerDataManager(uuid).get(FriendList).asInstanceOf[List[String]]

  def getOutgoingFriendRequests(uuid: UUID): List[String] =
    PlayerDataManager(uuid).get(OutgoingFriendRequests).asInstanceOf[List[String]]

  def getIncomingFriendRequests(uuid: UUID): List[String] =
    PlayerDataManager(uuid).get(IncomingFriendRequests).asInstanceOf[List[String]]

  def hasReceivedFriendRequestFrom(to: UUID, from: UUID): Boolean =
    getIncomingFriendRequests(to).contains(from.toString)

  def acceptRequest(to: UUID, from: UUID): Unit =
    PlayerDataManager(to)
      .removeFromStringList(IncomingFriendRequests, from.toString)
      .addToStringList(FriendList, from.toString)
      .save()
    PlayerDataManager(from)
      .removeFromStringList(OutgoingFriendRequests, to.toString)
      .addToStringList(FriendList, to.toString)
      .save()

  def denyRequest(to: UUID, from: UUID): Unit =
    PlayerDataManager(to)
      .removeFromStringList(IncomingFriendRequests, from.toString)
      .save()
    PlayerDataManager(from)
      .removeFromStringList(OutgoingFriendRequests, to.toString)
      .save()

  def removeFriend(player1: UUID, player2: UUID): Unit =
    PlayerDataManager(player1)
      .removeFromStringList(FriendList, player2.toString)
      .save()
    PlayerDataManager(player2)
      .removeFromStringList(FriendList, player1.toString)
      .save()