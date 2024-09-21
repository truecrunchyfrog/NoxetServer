package org.noxet.noxetserver.messaging

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.chat.hover.content.Text
import org.bukkit.World
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.noxet.noxetserver.NoxetServer
import org.noxet.noxetserver.realm.RealmManager.Realm
import org.noxet.noxetserver.messaging.channels.MessagingChannel
import org.noxet.noxetserver.util.TextBeautifier

import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters.*
import scala.collection.Seq

object Message:
  private val defaultPrefix = s"§b${TextBeautifier.beautify("no")}§3§lx§b${TextBeautifier.beautify("et")} §8§l| "

class Message:
  private val textComponents = ListBuffer[TextComponent]()
  private var prefix: Option[String] = Some(Message.defaultPrefix)
  protected var chatMessageType: ChatMessageType = ChatMessageType.CHAT

  /**
   * Constructs a message with a text.
   *
   * @param text The text message to be sent
   */
  def this(text: String) =
    this()
    add(text)

  def getDefaultColor: ChatColor = ChatColor.GRAY

  // Make prefix public and remove getters and setters?
  def getPrefix: String = prefix.get

  def setPrefix(newPrefix: Option[String]): Unit = prefix = newPrefix

  protected def bake: TextComponent =
    val mainComponent = TextComponent(prefix.getOrElse(""))

    textComponents.foreach(mainComponent.addExtra)

    mainComponent

  def add(text: String, hoverText: Option[String] = None, clickCommand: Option[String] = None): Message =
    if text == null then return this // TODO ????

    val textComponent = TextComponent(s"$text§r "); // Padding, for better transitions.

    textComponent.setColor(getDefaultColor)

    hoverText.foreach(v => textComponent.setHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, Text(v))))
    clickCommand.foreach(v => textComponent.setClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, s"/$v")))

    textComponents.addOne(textComponent)

    this

  def addButton(label: String, color: ChatColor, hoverText: Option[String], clickCommand: Option[String]): Message =
    val textComponent = TextComponent(s"→${TextBeautifier.beautify(label)} ")

    textComponent.setColor(color)
    textComponent.setBold(true)

    hoverText.foreach(v => textComponent.setHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, Text(v))))
    clickCommand.foreach(v => textComponent.setClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, s"/$v")))

    textComponents.addOne(textComponent)

    this

  def send(player: Player): Unit = player.spigot.sendMessage(chatMessageType, bake)

  def send(commandSender: CommandSender): Unit = commandSender match
    case p: Player => send(p)
    case _ => commandSender.spigot.sendMessage(bake)

  def send(players: Seq[Player]): Unit = players.foreach(send)

  def send(world: World): Unit = send(world.getPlayers.asScala)

  def send(worlds: => Seq[World]): Unit = worlds.foreach(send)

  def send(realm: Realm): Unit =
    if realm != null then send(realm.getWorlds.asScala)
    else send(NoxetServer.ServerWorld.HUB.getWorld)

  def send(channel: MessagingChannel): Unit = channel.getRecipients.forEach(send)

  def broadcast(): Unit = send(NoxetServer.getPlugin.getServer.getOnlinePlayers.asScala.toSeq)