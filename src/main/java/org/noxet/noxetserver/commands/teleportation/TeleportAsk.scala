package org.noxet.noxetserver.commands.teleportation

import org.bukkit.command.{Command, CommandSender, TabExecutor}
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.{ChatColor, Particle}
import org.noxet.noxetserver.commands.{PlayerTabExecutor, RegisteredCommand}
import org.noxet.noxetserver.commands.social.Friend
import org.noxet.noxetserver.commands.teleportation.TeleportAsk.requests
import org.noxet.noxetserver.messaging.ErrorMessage.ErrorType.{Argument, Common}
import org.noxet.noxetserver.messaging.{ErrorMessage, Message, NoteMessage, SuccessMessage}
import org.noxet.noxetserver.playerdata.PlayerDataManager
import org.noxet.noxetserver.realm.RealmManager
import org.noxet.noxetserver.util.{QuickRunnable, TeleportUtil, UsernameStorageManager}
import org.noxet.noxetserver.{Events, NoxetServer}

import scala.collection.mutable

object TeleportAsk extends PlayerTabExecutor, RegisteredCommand("tpa", this):
  private val requests = mutable.HashMap[Player, Player]()

  override def onPlayerCommand(player: Player, command: Command, s: String, strings: Array[String]): Boolean =
    RealmManager.getCurrentRealm(player) match
      case Some(realm) if !realm.allowTeleportationMethods =>
        ErrorMessage(Common, "This realm does not allow TPA.").send(player)
        true
      case Some(realm) =>
        strings match
          case Array() =>
            ErrorMessage(Argument, "Missing argument for player to send the request to (or cancel/accept/deny).").send(player)
            true
          case Array("cancel") =>
            if !requests.contains(player) then
              ErrorMessage(Common, "You have not sent any request.").send(player)
              return true

            val targetPlayer = requests.get(player)

            Message(s"§cYou aborted your teleport request to ${targetPlayer.getName}.").send(player)

            Message(s"§c${player.getName} aborted their teleportation request to you.").send(targetPlayer)

            requests.remove(player)

            true
          case Array("accept" | "deny", _*) if !requests.values.exists(_ == player) =>
            ErrorMessage(Common, "You have not received a teleportation request.").send(player)
            true
          case Array(action @ ("accept" | "deny"), "*") =>
            requests
              .filter(_._2 == player)
              .foreach((requester, target) =>
                target.performCommand(s"tpa $action ${requester.getName}"))
            true
          case Array("accept") =>
            requests.filter(_._2 == player) match
              case Seq(single) =>
                player.performCommand(s"tpa accept ${single.getName}")
              case Seq(first, _*) =>
                ErrorMessage(Common, "Ambiguous. More than one incoming TPA request. Please specify the player to accept.")
                  .addButton(
                    s"Accept from ${first.getName}",
                    ChatColor.GREEN,
                    Some("Accept the request from this player"),
                    Some(s"tpa accept ${first.getName}")
                  )
                  .addButton(
                    "List all requests",
                    ChatColor.YELLOW,
                    Some("See all your incoming requests"),
                    Some("tpa list")
                  )
                  .send(player)
                true
          case Array("accept" | "deny", playerName)
            if Option(NoxetServer.getPlugin.getServer.getPlayer(playerName)).isEmpty =>
            ErrorMessage(ErrorMessage.ErrorType.Common, "That player is not currently present on Noxet.").send(player)
            true
          case Array("accept" | "deny", playerName)
            if !requests.exists(
              _ == Option(NoxetServer.getPlugin.getServer.getPlayer(playerName)).get -> player
            ) =>
            ErrorMessage(ErrorMessage.ErrorType.Common,
              s"$playerName has not sent a teleportation request to you.").send(player)
            true
          case Array("accept", playerName) =>
            val acceptFrom = Option(NoxetServer.getPlugin.getServer.getPlayer(playerName)).get

            Message("§eAccepting request...").send(player)
            Message(s"§e${player.getName} accepted your teleportation request.").send(requester)

            if !TeleportUtil.isLocationTeleportSafe(player.getLocation) then
              ErrorMessage(ErrorMessage.ErrorType.Common,
                s"We blocked this teleportation because it appears to be unsafe. " +
                  s"${player.getName}, please move to a solid block and accept from there.")
                .send(List(player, requester))
              return true

            requests.remove(acceptFrom)

            Events.setTemporaryInvulnerability(player)
            acceptFrom.teleport(player)
            acceptFrom.getWorld.spawnParticle(
              Particle.DRAGON_BREATH, acceptFrom.getLocation, 56)

            Message(s"§e${requester.getName} has teleported to you.").send(player)
            Message("§eYou have been teleported.").send(requester)

            true
          case Array("deny", playerName) =>
            val requester = Option(NoxetServer.getPlugin.getServer.getPlayer(playerName)).get

            requests.remove(requester)

            Message(s"§eYou denied the teleportation request from ${requester.getName}.").send(player)
            Message(s"§e${player.getName} denied your teleportation request.").send(requester)

            true
          case Array("list") =>
            val incomingRequests = requests.filter(_._2 == player)

            Message(s"§6Pending incoming teleportation requests: ${incomingRequests.size}").send(player)

            incomingRequests.foreach(
              Message(s"└§a§lINCOMING §6${incoming.getName}")
                .addButton(
                  "Accept",
                  ChatColor.GREEN,
                  "Accept this request",
                  s"tpa accept ${incoming.getName}"
                ).send(player))

            requests.get(player) match
              case Some(outgoingRequest) =>
                Message(s"└§2§lOUTGOING §6${outgoingRequest.getName}")
                  .addButton(
                    "Cancel",
                    ChatColor.GRAY,
                    "Cancel your teleportation request",
                    "tpa cancel"
                  ).send(player)
              case None => ()
            true
          case Array("toggle-allow-incoming") =>
            val playerDataManager = PlayerDataManager(player)
            val setTo = !playerDataManager
              .get(PlayerDataManager.Attribute.DisallowIncomingTpaRequests).asInstanceOf[Boolean]
            playerDataManager.set(PlayerDataManager.Attribute.DisallowIncomingTpaRequests, setTo)

            SuccessMessage(
              s"${if setTo then "Disabled" else "Enabled"} incoming TPA requests. " +
                s"Other players can ${if setTo then "no longer" else "now"} TPA to you.")
              .send(player)

            if setTo && requests.containsValue(player) then
              NoteMessage(
                "You already have incoming TPA requests pending. " +
                  "These are kept. New requests will be canceled.").send(player)

            true
          case Array(playerName)
            if Option(NoxetServer.getPlugin.getServer.getPlayer(playerName)).isEmpty =>
            ErrorMessage(ErrorMessage.ErrorType.Common, "That player is not currently present on Noxet.").send(player)
            true
          case Array(playerName) =>
            if requests.contains(player) then
              ErrorMessage(Common,
                "You have already made a teleportation request. " +
                  "Cancel it before you can make another one.")
                .send(player)
              return true

            val target = Option(NoxetServer.getPlugin.getServer.getPlayer(playerName)).get

            if target == player then
              ErrorMessage(Common, "You cannot teleport to yourself!").send(player)
              return true

            RealmManager.getCurrentRealm(target) match
              case Some(`realm`) => ()
              case Some(targetRealm) =>
                ErrorMessage(Common,
                  s"${target.getName} is not in $realm. " +
                    s"They are in $targetRealm! You must move to that realm first.").send(player)
                return true
              case None =>
                ErrorMessage(Common, s"${target.getName} is not in a realm.")
                  .send(player)
                return true

            if PlayerDataManager(target).doesContain(
              PlayerDataManager.Attribute.BlockedPlayers, player.getUniqueId.toString) ||
              PlayerDataManager(target).get(PlayerDataManager.Attribute.DisallowIncomingTpaRequests).asInstanceOf[Boolean]
            then
              ErrorMessage(Common, "You may not send TPA requests to this player.").send(player)
              return true

            requests.put(player, target)

            Message(s"§eSent a teleportation request to ${target.getName}.").send(player)
            Message(null).addButton("Cancel", ChatColor.RED, "Cancel your request", "tpa cancel").send(player)

            Message(s"§6${player.getName}§e has sent you a teleportation request.").send(target)
            Message(null)
              .addButton(
                "Accept",
                ChatColor.GREEN,
                s"Teleport ${player.getName} to you",
                s"tpa accept ${player.getName}")
              .addButton(
                "Deny",
                ChatColor.RED,
                "Deny this request",
                s"tpa deny ${player.getName}")
              .send(targetPlayer)

            if Friend.areFriends(player, target) &&
              PlayerDataManager(targetPlayer)
                .get(PlayerDataManager.Attribute.FriendTeleportation).asInstanceOf[Boolean]
            then
              Message(
                s"§b${targetPlayer.getName} has friendly teleportation enabled. " +
                  "They will automatically accept in §35§b seconds.").send(player)
              Message(s"§b${player.getName} is your friend, " +
                s"and you have friendly teleportation enabled. " +
                s"The request will be automatically accepted in §35§b seconds.").send(targetPlayer)

              QuickRunnable(
                targetPlayer.performCommand(s"tpa accept ${player.getName}"))
                .runTaskLater(NoxetServer.getPlugin, 20 * 5)

            QuickRunnable(() =>
              if requests.remove(player, targetPlayer) then // If value was removed; was still active.
                // Meaning that the request was still valid. But now it has expired.
                Message(s"§cYour teleportation request to ${target.getDisplayName} has expired.")
                  .send(player)
            ).runTaskLater(NoxetServer.getPlugin, 20 * 60)

            true
          case _ =>
            ErrorMessage(Common,
              "You must be in a realm to do this.").send(player)
            true

  def abortPlayerRelatedRequests(player: Player): Unit =
    if requests.contains(player) then
      Message(
        s"§cThe teleportation request from ${player.getName} has been aborted " +
          "because they left the realm.")
        .send(requests.get(player))
      requests.remove(player) // Remove request FROM player.

    val incomingRequests = requests.filter(_._2 == player)
    incomingRequests
      .foreach((requester, target) =>
        Message(
          s"§cYour teleportation request to ${target.getName} has " +
            "been aborted because they left the realm.")
          .send(requester)
        requests.remove(requester) // Remove requests TO player.
      )

  override def onPlayerTabComplete(player: Player, command: Command, s: String, strings: Array[String]): List[String] =
    strings match
      case Array(_) =>
        List("cancel", "accept", "deny", "list") :::
          RealmManager.getCurrentRealm(player) match
          case Some(realm) =>
            realm.getPlayers.excl(player).map(_.getName)
          case None => List()
      case Array(_, action) => action match // TODO is it not the first one?
        case "accept" | "deny" =>
          List("*") ::: requests
            .filter(_._2 == player)
            .map(_._1.getName)
            .toList
        case _ => List()
      case _ => List()