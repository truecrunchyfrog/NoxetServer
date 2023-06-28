package org.noxet.noxetserver.util;

import org.bukkit.entity.Player;
import org.bukkit.util.Consumer;

import java.util.*;

public class TeamSet {
    private final List<Team> teams;
    private final Set<Player> playerSet;
    private final Map<Player, Team> assignedPlayerTeams;

    public TeamSet(Set<Player> playerSet, Team... teams) {
        this.playerSet = playerSet;
        this.teams = Arrays.asList(teams);
        assignedPlayerTeams = new HashMap<>();
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
    }

    public void putManyPlayersOnTeam(Set<Player> players, Team team) {
        for(Player player : players)
            putPlayerOnTeam(player, team);
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
    }
}
