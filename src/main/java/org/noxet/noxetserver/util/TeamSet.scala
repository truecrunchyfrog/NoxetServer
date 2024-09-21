package org.noxet.noxetserver.util

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scoreboard.*
import org.noxet.noxetserver.menus.inventory.TeamPickerMenu

import scala.annotation.tailrec
import scala.collection.mutable
import scala.jdk.CollectionConverters.*

class TeamSet(val playerSet: Set[Player], val teams: Team*):
  private val scoreboard = Bukkit.getScoreboardManager.getNewScoreboard
  private val objective = scoreboard
    .registerNewObjective("game_stats", Criteria.DUMMY, "§6§lWORLD§2§lEATER")
  objective.setDisplaySlot(DisplaySlot.SIDEBAR)

  private val assignedPlayerTeams: mutable.Map[Player, Team] = mutable.HashMap()

  for team <- teams do
    val scoreboardTeam = scoreboard.registerNewTeam(team.getTeamId)

    scoreboardTeam.setOption(org.bukkit.scoreboard.Team.Option.NAME_TAG_VISIBILITY, org.bukkit.scoreboard.Team.OptionStatus.FOR_OTHER_TEAMS)
    scoreboardTeam.setCanSeeFriendlyInvisibles(false)
    scoreboardTeam.setAllowFriendlyFire(false)

    scoreboardTeam.setPrefix(team.getFormattedDisplayName)
    scoreboardTeam.setColor(team.getColor)

  def getTeams: List[Team] = teams.toList

  private def updateTeamEntriesAndPlayerScoreboards(): Unit =
    for (player, team) <- assignedPlayerTeams do
      player.setScoreboard(scoreboard)
      scoreboard.getTeam(team.getTeamId).addEntry(player.getName)

  def getPlayersTeam(player: Player): Option[Team] =
    assignedPlayerTeams.get(player)

  def isPlayerOnTeam(player: Player, team: Team): Boolean =
    if !teams.contains(team) then
      throw IllegalArgumentException(s"Team $team is not part of this TeamSet.")

    getPlayersTeam(player) == team

  def putPlayerOnTeam(player: Player, team: Team): Unit =
    if !teams.contains(team) then
      throw IllegalArgumentException(s"Team $team is not part of this TeamSet.")

    if !playerSet.contains(player) then
      throw IllegalArgumentException(s"Player $player does not exist in the referenced player set.")

    assignedPlayerTeams.put(player, team)

    updateTeamEntriesAndPlayerScoreboards()

  def putManyPlayersOnTeam(players: Set[Player], team: Team): Unit =
    players.foreach(putPlayerOnTeam(_, team))

  def assignPlayersByTeamPickerMenu(menu: TeamPickerMenu): Unit =
    menu.getPlayerTeams.foreach(putManyPlayersOnTeam(_._1, _._2))

  def getPlayersOnTeam(team: Team): Set[Player] =
    if !teams.contains(team) then
      throw IllegalArgumentException(s"Team $team is not part of this TeamSet.")
    assignedPlayerTeams.filter(_._2 == team).keys.toSet

  def countTeamPlayers(team: Team): Int = getPlayersOnTeam(team).size

  def isTeamEmpty(team: Team): Boolean = getPlayersOnTeam(team).isEmpty

  def forEach(team: Team, f: Player => Unit): Unit =
    getPlayersOnTeam(team).foreach(f)

  /**
   * Should be called when a player has been removed from the game.
   */
  def refreshPlayers(): Unit =
    assignedPlayerTeams.filterInPlace((p, _) => playerSet.contains(p))
    updateTeamEntriesAndPlayerScoreboards()

  def updateScoreboard(lines: String*): Unit =
    scoreboard.getEntries.asScala.foreach(scoreboard.resetScores)

    for (line, i) <- lines.zipWithIndex do
      // Duplicate score names are not possible in scoreboards,
      // so we prepend the content with the reset code (§r) to bypass this.
      @tailrec
      def findFreeLine(content: String): String =
        if objective.getScore(content).isScoreSet then content
        else findFreeLine("§r" + content)

      objective.getScore(findFreeLine(line)).setScore(lines.size - i)

  def unregister(): Unit =
    objective.unregister()
    scoreboard.getTeams.asScala.foreach(_.unregister)