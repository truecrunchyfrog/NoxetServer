package org.noxet.noxetserver.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;
import org.bukkit.util.Consumer;
import org.noxet.noxetserver.menus.inventory.TeamPickerMenu;

import java.util.*;

public class TeamSet {
    private final Scoreboard scoreboard;
    private final Objective objective;

    private final List<Team> teams;
    private final Set<Player> playerSet;
    private final Map<Player, Team> assignedPlayerTeams;

    public TeamSet(Set<Player> playerSet, Team... teams) {
        this.playerSet = playerSet;
        this.teams = Arrays.asList(teams);
        assignedPlayerTeams = new HashMap<>();

        scoreboard = Objects.requireNonNull(Bukkit.getScoreboardManager()).getNewScoreboard();

        objective = scoreboard.registerNewObjective("game_stats", Criteria.DUMMY, "§6§lWORLD§2§lEATER");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        for(Team team : teams) {
            org.bukkit.scoreboard.Team scoreboardTeam = scoreboard.registerNewTeam(team.getTeamId());

            scoreboardTeam.setOption(org.bukkit.scoreboard.Team.Option.NAME_TAG_VISIBILITY, org.bukkit.scoreboard.Team.OptionStatus.FOR_OTHER_TEAMS);
            scoreboardTeam.setCanSeeFriendlyInvisibles(false);
            scoreboardTeam.setAllowFriendlyFire(false);

            scoreboardTeam.setPrefix(team.getFormattedDisplayName());
            scoreboardTeam.setColor(team.getColor());
        }
    }

    public List<Team> getTeams() {
        return teams;
    }

    private void updateTeamEntriesAndPlayerScoreboards() {
        for(Map.Entry<Player, Team> entry : assignedPlayerTeams.entrySet()) {
            entry.getKey().setScoreboard(scoreboard);
            Objects.requireNonNull(scoreboard.getTeam(entry.getValue().getTeamId())).addEntry(entry.getKey().getName());
        }
    }

    public Team getPlayersTeam(Player player) {
        return playerSet.contains(player) ? assignedPlayerTeams.get(player) : null;
    }

    public boolean isPlayerOnTeam(Player player, Team team) {
        if(!teams.contains(team))
            throw new IllegalArgumentException("Team " + team + " is not part of this TeamSet.");

        return getPlayersTeam(player) == team;
    }

    public void putPlayerOnTeam(Player player, Team team) {
        if(!teams.contains(team))
            throw new IllegalArgumentException("Team " + team + " is not part of this TeamSet.");

        if(!playerSet.contains(player))
            throw new IllegalArgumentException("Player " + player + " does not exist in the referenced player set.");

        assignedPlayerTeams.put(player, team);

        updateTeamEntriesAndPlayerScoreboards();
    }

    public void putManyPlayersOnTeam(Set<Player> players, Team team) {
        for(Player player : players)
            putPlayerOnTeam(player, team);
    }

    public void assignPlayersByTeamPickerMenu(TeamPickerMenu menu) {
        for(Map.Entry<Team, Set<Player>> teamSetEntry : menu.getPlayerTeams().entrySet()) {
            Team team = teamSetEntry.getKey();
            Set<Player> players = teamSetEntry.getValue();

            putManyPlayersOnTeam(players, team);
        }
    }

    public Set<Player> getPlayersOnTeam(Team team) {
        if(!teams.contains(team))
            throw new IllegalArgumentException("Team " + team + " is not part of this TeamSet.");

        Set<Player> playersOnTeam = new HashSet<>();
        for(Map.Entry<Player, Team> playerTeamEntry : assignedPlayerTeams.entrySet())
            if(playerTeamEntry.getValue() == team)
                playersOnTeam.add(playerTeamEntry.getKey());

        return playersOnTeam;
    }

    public int countTeamPlayers(Team team) {
        return getPlayersOnTeam(team).size();
    }

    public boolean isTeamEmpty(Team team) {
        return countTeamPlayers(team) == 0;
    }

    public void forEach(Team team, Consumer<Player> consumer) {
        for(Player player : getPlayersOnTeam(team))
            consumer.accept(player);
    }

    /**
     * Should be called when a player has been removed from the game.
     */
    public void refreshPlayers() {
        assignedPlayerTeams.keySet().retainAll(playerSet);

        updateTeamEntriesAndPlayerScoreboards();
    }

    private void setObjectiveLines(String... lines) {
        for(String score : scoreboard.getEntries())
            scoreboard.resetScores(score);

        int i = 0;
        for(String line : lines) {
            Score score;

            StringBuilder lineBuilder = new StringBuilder(line);
            do {
                lineBuilder.insert(0, "§r");
                score = objective.getScore(lineBuilder.toString()); // Duplicate scores are not possible in scoreboards, so we use the reset code (§r) to bypass this.
            } while(score.isScoreSet());

            score.setScore(lines.length - i++);
        }
    }

    public void updateScoreboard(String... lines) {
        setObjectiveLines(lines);
    }

    public void unregister() {
        objective.unregister();
        for(org.bukkit.scoreboard.Team team : scoreboard.getTeams())
            team.unregister();
    }
}
