package org.noxet.noxetserver.menus.chat

import org.bukkit.entity.Player
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.{EventHandler, EventPriority, HandlerList, Listener}
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.Consumer
import org.noxet.noxetserver.NoxetServer
import org.noxet.noxetserver.messaging.Message

import scala.collection.{Set, mutable}

class ChatPromptMenu(message: String, promptedPlayers: Set[Player], callback: Consumer[ChatPromptMenu.PromptResponse]) extends Listener:
    private val timeout = new BukkitRunnable {
        override def run(): Unit =
            promptedPlayers.filter(_.isOnline).foreach(dispatchPlayerMessage(_, ""))
            stop()
    }.runTaskLater(NoxetServer.getPlugin, 20 * 60)

    NoxetServer.getPlugin.getServer.getPluginManager.registerEvents(this, NoxetServer.getPlugin)

    promptedPlayers.foreach(promptPlayer)
    private val notAnswered: mutable.Set[Player] = promptedPlayers.to(mutable.HashSet.iterableFactory)

    def this(message: String, promptedPlayer: Player, callback: Consumer[ChatPromptMenu.PromptResponse]) =
        this(message, Set(promptedPlayer), callback)

    private def promptPlayer(player: Player): Unit =
        Message("§8" + "■".repeat(40) + "§3Enter §b" + message + "§3:").send(player)

    @EventHandler(priority = EventPriority.LOW)
    def onAsyncPlayerChat(e: AsyncPlayerChatEvent): Unit =
        if promptedPlayers.contains(e.getPlayer) then
            new BukkitRunnable {
                override def run(): Unit = dispatchPlayerMessage(e.getPlayer, e.getMessage)
            }.runTaskLater(NoxetServer.getPlugin, 0)
            e.setCancelled(true)

    private def dispatchPlayerMessage(player: Player, value: String): Unit =
        notAnswered.remove(player) match
            case true => ()
            case false if promptedPlayers.contains(player) => throw new NoSuchElementException("cannot dispatch player's message because they have already answered")
            case _ => throw new NoSuchElementException("cannot dispatch player's message because they are not part of this prompt")
        if notAnswered.isEmpty then stop()
        callback.accept(ChatPromptMenu.PromptResponse(player, value))

    def stop(): Unit =
        HandlerList.unregisterAll(this)
        timeout.cancel()

object ChatPromptMenu:
    case class PromptResponse(player: Player, response: String)