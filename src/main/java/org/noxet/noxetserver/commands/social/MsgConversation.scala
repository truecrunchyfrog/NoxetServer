package org.noxet.noxetserver.commands.social

import org.bukkit.command.{Command, CommandSender, TabExecutor}
import org.bukkit.entity.Player
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.{EventHandler, Listener}
import org.noxet.noxetserver.NoxetServer
import org.noxet.noxetserver.commands.RegisteredCommand
import org.noxet.noxetserver.commands.social.MsgConversation.MessageDirectionType.{Incoming, Outgoing}
import org.noxet.noxetserver.commands.social.MsgConversation.playerConversationChannels
import org.noxet.noxetserver.messaging.*
import org.noxet.noxetserver.playerdata.PlayerDataManager
import org.noxet.noxetserver.util.{TextBeautifier, UsernameStorageManager}

import scala.collection.immutable.{AbstractSeq, LinearSeq}
import scala.collection.mutable

object MsgConversation extends PlayerTabExecutor, RegisteredCommand("msg", this), Listener:
  NoxetServer.getPlugin.getServer.getPluginManager.registerEvents(this, NoxetServer.getPlugin)

  private val playerConversationChannels: mutable.Map[Player, Player] = mutable.HashMap()

  private def mayPlayerChatWithPlayer(askingPlayer: UUID, targetPlayer: UUID): Boolean =
    val askingData = PlayerDataManager(askingPlayer)
    val targetData = PlayerDataManager(targetPlayer)
    !targetData.get(PlayerDataManager.Attribute.MsgDisabled).asInstanceOf[Boolean] &&
      !targetData.doesContain(PlayerDataManager.Attribute.BlockedPlayers, askingPlayer.toString) &&
      !askingData.doesContain(PlayerDataManager.Attribute.BlockedPlayers, targetPlayer.toString)

  private enum MessageDirectionType:
    case Incoming, Outgoing

  private def getConversationMessage(oppositePlayer: Player, direction: MessageDirectionType, message: String): Message =
    val (text, hoverText) = direction match
      case Incoming =>
        (
          s"§7✉→ ${TextBeautifier.beautify("from")} §d${oppositePlayer.getName}§5◇ §f$message",
          s"${oppositePlayer.getName} sent this (use /msg ${oppositePlayer.getName} <message> to reply)"
        )
      case Outgoing =>
        (
          s"§3→✉ ${TextBeautifier.beautify("to")}  ",
          "You sent this"
        )
    Message.add(text, hoverText)

  override def onPlayerCommand(player: Player, command: Command, s: String, strings: Array[String]): Boolean =
    val playerDataManager = PlayerDataManager(player)

    strings match
      case Array() =>
        ErrorMessage(ErrorMessage.ErrorType.Argument,
          "Missing argument (player to message or toggle/block).").send(player)
        true
      case Array("toggle") =>
        val isMsgDisabled = playerDataManager
          .get(PlayerDataManager.Attribute.MsgDisabled).asInstanceOf[Boolean]

        playerDataManager.set(PlayerDataManager.Attribute.MsgDisabled, !isMsgDisabled).save()

        SuccessMessage(
          s"Noxet direct messaging was ${
            if isMsgDisabled then
              "enabled (you can now /msg players)"
            else
              "disabled (you can no longer /msg players)"
          }.")
          .send(player)

        true
      case Array(identifier, args*) =>
        if playerDataManager.get(PlayerDataManager.Attribute.MsgDisabled).asInstanceOf[Boolean] then
          ErrorMessage(ErrorMessage.ErrorType.Common, "You have disabled messaging.").send(player)
          return true

        PlayerIntel(identifier) match
          case Some(PlayerIntel(_, None, _)) =>
            ErrorMessage(ErrorMessage.ErrorType.Common, "That player is not online.").send(player)
            true
          case Some(PlayerIntel(_, Some(`player`), _)) =>
            ErrorMessage(ErrorMessage.ErrorType.Common, "You cannot message yourself!").send(player)
            true
          case Some(recipient@PlayerIntel(_, Some(recipientPlayer), _)) =>
            if !mayPlayerChatWithPlayer(player, recipient) then
              ErrorMessage(ErrorMessage.ErrorType.Common, "You cannot message this player.").send(player)
              return true

            val messageToSend =
              if args.nonEmpty then
                Some(args.mkString(""))
              else
                None

            val targetPlayerDataManager = PlayerDataManager(recipient)

            val playerSpokenToList = playerDataManager
              .get(PlayerDataManager.Attribute.MsgSpokenTo)
              .asInstanceOf[List[String]]

            if !targetPlayerDataManager
              .get(PlayerDataManager.Attribute.MsgSpokenTo)
              .asInstanceOf[List[String]]
              .contains(player.getUniqueId.toString) &&
              !Friend.areFriends(player, recipient)
            then
              // Target player does not have this player in their conversation list.

              if playerSpokenToList.contains(recipient.uuid.toString) then
                // This player has the target player in their conversation list.
                // This means that this player has already taken contact with the target player, and should not be
                // allowed to contact again until they receive a reply from target.

                ErrorMessage(ErrorMessage.ErrorType.Common,
                  "Please wait for them to reply to your first message before you can send another.")
                  .send(player)
                return true

              if messageToSend.isEmpty then
                ErrorMessage(ErrorMessage.ErrorType.Argument,
                  "You must initiate the new conversation with a message. Missing message.")
                  .send(player)
                return true

              // Create a new message request:

              playerDataManager
                .addToStringList(PlayerDataManager.Attribute.MsgSpokenTo, recipient.uuid.toString)
                .save()

              getConversationMessage(player, Incoming, messageToSend)
                .addButton(
                  "Greet",
                  ChatColor.GREEN,
                  "Send a greeting reply and allow them to talk to you",
                  s"msg ${player.getName} Greetings!"
                )
                .send(recipientPlayer)

              NoteMessage(s"${player.getName} just messaged you for the first time.\n" +
                "You need to reply first before they can send another message.\n" +
                "If you don't want to talk to them, " +
                "simply ignore their message and they cannot message you again.")
                .send(recipientPlayer)

              getConversationMessage(recipientPlayer, Outgoing, messageToSend).send(player)

              NoteMessage(s"$recipient needs to reply before " +
                s"you can continue the conversation with them.").send(player)
              return true

            if !playerSpokenToList.contains(recipient.uuid.toString) &&
              !Friend.areFriends(player, recipient) then
              // This counts as a reply. Target player has been accepted and conversation created.

              playerSpokenToList.add(recipient.uuid.toString)

              playerDataManager.set(PlayerDataManager.Attribute.MsgSpokenTo, playerSpokenToList)
                .save()

              SuccessMessage(s"${player.getName} accepted the conversation with you. You can now message them.").send(recipient)
              SuccessMessage(s"$recipient can now message you.").send(player)

            if messageToSend.isDefined then
              // Send message:
              getConversationMessage(recipientPlayer, Outgoing, messageToSend).send(player)
              getConversationMessage(player, Incoming, messageToSend).send(recipient)
            else
              // Toggle conversation mode:
              if playerConversationChannels.remove(player, recipientPlayer) then
                Message(s"§cYou exited conversation mode with §f$recipient§c.").send(player)
                return true

              playerConversationChannels.put(player, recipientPlayer)
              Message(s"§3Entered conversation mode. " +
                s"Messages you send will be messaged privately to §d$recipient§3.")
                .addButton("Exit", ChatColor.RED, "Exit conversation mode", s"msg $recipient").send(player)

            true
          case None =>
            ErrorMessage(
              ErrorMessage.ErrorType.Common,
              "That player is not registered on Noxet.")
              .send(player)
            true

  def clearConversationChannels(player: Player): Unit =
    // Remove possible outgoing channel:
    playerConversationChannels.remove(player)
    // Remove possible incoming channels:
    playerConversationChannels.filterInPlace((_, recipient) =>
      if recipient == player then
        Message(s"§eYou exit conversation mode because ${player.getName} left.")
          .send(p1)
        false
      else
        true
    )

  override def onPlayerTabComplete(player: Player, command: Command, s: String, strings: Array[String]): List[String] =
    strings match
      case Array("") =>
        Message("§5Enter a player to message, or an action from the menu.")
          .send(commandSender)
        List()
      case Array(playerName) =>
        "toggle" + Option(NoxetServer.getPlugin.getServer.getPlayer(playerName))
          .map(_.getName)
          .toList
      case Array(playerName, "") =>
        Message(s"§5Enter a message to send to §f$playerName§5:").send(commandSender)
        List()
      case _ => List()

  @EventHandler def onAsyncPlayerChat(e: AsyncPlayerChatEvent): Unit =
    playerConversationChannels.get(e.getPlayer) match
      case Some(recipient) =>
        e.getPlayer.performCommand(s"msg ${recipient.getName} ${e.getMessage}")
        e.setCancelled(true)
      case None => ()