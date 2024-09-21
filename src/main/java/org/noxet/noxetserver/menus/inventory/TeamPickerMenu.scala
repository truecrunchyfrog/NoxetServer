package org.noxet.noxetserver.menus.inventory

import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.scheduler.{BukkitRunnable, BukkitTask}
import org.bukkit.util.Consumer
import org.bukkit.{Material, Sound}
import org.noxet.noxetserver.NoxetServer
import org.noxet.noxetserver.menus.ItemGenerator
import org.noxet.noxetserver.menus.inventory.TeamPickerMenu.fillTeams
import org.noxet.noxetserver.messaging.ErrorMessage
import org.noxet.noxetserver.minigames.MiniGameController
import org.noxet.noxetserver.util.{InventoryCoordinate, InventoryCoordinateUtil, QuickRunnable, Team}

import scala.annotation.tailrec
import scala.collection.mutable

object TeamPickerMenu:
  /**
   * To fill the teams, we step through each team for every player so it is fairly divided.
   * We ascend and leave one player and move on (and skip full teams).
   */
  @tailrec
  private def fillTeams(players: List[Player], teams: LazyList[Team]): mutable.Map[Team, mutable.Set[Player]] =
    players.headOption match
      case Some(player) =>
        fillTeams(
          players.drop(1), // Next player
          teams.drop(1) // Next team
            .dropWhile(t => t.getMaxTeamMembers <= playerTeams(t).size)) // Skip full teams
          .updatedWith(mapping => Some(mapping.getOrElse(mutable.Set()) + player))
      case None => mutable.HashMap() // Done

class TeamPickerMenu(
                      private val game: MiniGameController,
                      teams: List[Team],
                      secondsToWait: Int,
                      private val callback: TeamPickerMenu => Unit)
  extends InventoryMenu(teams.size + 1, "Choose a Team", true):
  val playerTeams: mutable.Map[Team, mutable.Set[Player]] = fillTeams(game.getPlayers, teams.to(LazyList))
  private val readyPlayers: mutable.Set[Player] = mutable.HashSet()
  private val timeSlot = InventoryCoordinate(0, getInventory.getSize / 9 - 1)
  private val readySlot = InventoryCoordinate(8, getInventory.getSize / 9 - 1)

  if teams.map(_.getMaxTeamMembers).sum < game.getPlayers.size then
    throw IllegalStateException(
      "Cannot create team picker menu for a game with more players than the teams together can fit!")

  private var timeLeft = secondsToWait
  private val timer = QuickRunnable(() =>
    timeLeft -= 1
    if timeLeft == 0 || game.hasEnded then
      stop()
    updateInventory()
  ).runTaskTimer(NoxetServer.getPlugin, 120, 20)

  override protected def updateInventory(): Unit =
    disbandRemovedPlayers()

    for ((team, players), y) <- playerTeams.zipWithIndex do
      setSlotItem(ItemGenerator.generateItem(
        team.getTeamIcon,
        s"§7Team${if team.getMaxTeamMembers > teamPlayers.size then "" else " §c(full!)"}",
        List(team.getFormattedDisplayName + 'S')
      ), 0, y)

      val showPlayers =
        if players.size <= 8 then players
        else players.to(LazyList).slice(secondsToWait - timeLeft, secondsToWait - timeLeft + 8)

      for (player, i) <- showPlayers.zipWithIndex do
        setSlotItem(ItemGenerator.generatePlayerSkull(
          player,
          s"${team.getFormattedDisplayName}: §b${player.getName}",
          None
        ), i + 1, y)

      // Fill rest of the row.
      for x <- (showPlayers.size + 1) until 9 do
        setSlotItem(ItemGenerator.generateItem(
          Material.LIGHT_GRAY_STAINED_GLASS_PANE,
          if team.getMaxTeamMembers > teamPlayers.size then
            "§7Click to play as a"
          else
            s"§c${if timeLeft % 3 == 0 then "§n" else ""}This team is full!",
          List(team.getFormattedDisplayName)
        ), x, y)

    setSlotItem(ItemGenerator.generateItem(
      if timeLeft % 2 == 0 then Material.CLOCK else Material.LIGHT_GRAY_STAINED_GLASS_PANE,
      timeLeft,
      s"§c$timeLeft§es",
      List("§euntil start.", "§aReady up to skip waiting.")
    ), timeSlot)

    val notReadyPlayers = getPlayers.diff(readyPlayers)

    setSlotItem(ItemGenerator.generateItem(
      Material.GREEN_CONCRETE,
      notReadyPlayers.size,
      s"§e${readyPlayers.size}/${getPlayers.size} players ready.",

      List("§eWaiting for:") :::
        notReadyPlayers.map(s"§7 - §c${p.getName}") :::
        List("§dSee yourself in the list? Click here to start early.")
    ), readySlot)

  override protected def onSlotClick(player: Player, coordinate: InventoryCoordinate, clickType: ClickType): Boolean =
    disbandRemovedPlayers()

    if coordinate == readySlot then
      readyPlayers.add(player)

      if readyPlayers.size == getPlayers.size then
        return true // Start game, and stop menu - if all are ready.

    // TODO have some more proper way of indexing
    // using nothing but a map is probably not very clever
    // probably have some other list that keeps track of indices
    playerTeams.zipWithIndex.find(_._2 == coordinate.y).map((v, _) => v) match
      case Some((team, players)) if players.size < team.getMaxTeamMembers =>
        readyPlayers.remove(player)
        playerTeams.values.foreach(_.remove(player))
        players.add(player)
        updateInventory()
      case Some(_) =>
        ErrorMessage(ErrorMessage.ErrorType.Common, "This team is full!").send(player)
        player.playSound(player, Sound.ENTITY_VILLAGER_NO, 1, 0.5f)
      case _ => ()

    false

  override protected def stop(): Unit =
    disbandRemovedPlayers()

    timer.cancel()
    if !game.hasEnded then
      callback(this)

    super.stop()

  private def getPlayers: Set[Player] = playerTeams.values.flatten.toSet

  def disbandRemovedPlayers(): Unit =
    playerTeams.values.foreach(_.filterInPlace(!game.isPlayer))
    readyPlayers.filterInPlace(!game.isPlayer)