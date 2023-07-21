package org.noxet.noxetserver.minigames;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;
import org.noxet.noxetserver.messaging.ErrorMessage;
import org.noxet.noxetserver.messaging.WarningMessage;
import org.noxet.noxetserver.minigames.party.Party;

import java.util.HashSet;
import java.util.Set;

public class MiniGameManager {
    private static final Set<MiniGameController> registeredGames = new HashSet<>();

    public static void registerGame(MiniGameController miniGame) {
        registeredGames.add(miniGame);
    }

    public static void unregisterGame(MiniGameController miniGame) {
        registeredGames.remove(miniGame);
    }

    public static Set<MiniGameController> getRegisteredGames() {
        return registeredGames;
    }

    public static Set<MiniGameController> findGames(GameDefinition gameDefinition) {
        Set<MiniGameController> foundGames = new HashSet<>();

        for(MiniGameController eachGame : registeredGames)
            if(eachGame.getGame() == gameDefinition)
                foundGames.add(eachGame);

        return foundGames;
    }

    /**
     * Find the game a player is playing in.
     * @param player The player to search for
     * @return The game instance with the player if found, otherwise null
     */
    public static MiniGameController findPlayersGame(Player player) {
        for(MiniGameController eachGame : registeredGames)
            if(eachGame.isPlayer(player))
                return eachGame;

        return null;
    }

    /**
     * Find the game a player is spectating.
     * @param player The spectator to search for
     * @return The game instance with the spectator if found, otherwise null
     */
    public static MiniGameController findSpectatorsGame(Player player) {
        for(MiniGameController eachGame : registeredGames)
            if(eachGame.isSpectator(player))
                return eachGame;

        return null;
    }

    /**
     * Find the game a player is either playing in or spectating.
     * @param player The player who is playing or spectating
     * @return The game instance with the player/spectator if found, otherwise null
     */
    public static MiniGameController findPlayersOrSpectatorsGame(Player player) {
        MiniGameController playingIn = findPlayersGame(player);
        MiniGameController spectating = findSpectatorsGame(player);

        return playingIn != null ? playingIn : spectating;
    }

    /**
     * Check if a player is currently in a game that has not ended.
     * This will only check if the player is participating, not spectating!
     * @param player The player to check for
     * @return true if the player is currently participating in a game that has not yet ended, otherwise false
     */
    public static boolean isPlayerBusyInGame(Player player) {
        MiniGameController playersGame = findPlayersGame(player);

        return playersGame != null && !playersGame.hasEnded();
    }

    public static void playGame(Player player, GameDefinition game) {
        Party party = Party.getPartyFromMember(player);

        if(party != null) {
            if(!party.isOwner(player)) {
                new ErrorMessage(ErrorMessage.ErrorType.COMMON, "You are in a party. Only the party owner can join games for the party.\nYou can leave the party to play by yourself, or have the owner transfer the ownership to you to start games yourself.").send(player);
                return;
            }

            partyPlayGame(party, game);
            return;
        }

        Set<MiniGameController> games = findGames(game);

        MiniGameController selectedGame = null;

        for(MiniGameController eachGame : games)
            if(!eachGame.isFull() && (!eachGame.hasStarted() || (eachGame.isPlaying() && eachGame.getOptions().allowPlayerDropIns())) && (selectedGame == null ||
                    (
                            (selectedGame.enoughPlayers() &&
                            !eachGame.enoughPlayers()) || // Prioritize this game, with not enough players.
                            (eachGame.getPlayers().size() > selectedGame.getPlayers().size()) // Prioritize filling up this game before others.
                    ))
            )
                selectedGame = eachGame;

        if(selectedGame == null)
            selectedGame = createNewGame(game);

        assert selectedGame != null;

        selectedGame.addPlayer(player);
    }

    public static void partyPlayGame(Party party, GameDefinition game) {
        if(game.getOptions().getMaxPlayers() < party.getMembers().size()) {
            party.sendPartyMessage(new ErrorMessage(ErrorMessage.ErrorType.COMMON, "Tried to join a game of " + game.getOptions().getDisplayName() + ", but there are too many players in this party. The game can only allow " + game.getOptions().getMaxPlayers() + " players, but there are " + party.getMembers().size() + " players in this party."));
            return;
        }

        if(!party.isPartyReadyForGame()) {
            new WarningMessage("Cannot join game, because all members may not be ready.")
                    .addButton(
                            "Kick busy players (" + party.getBusyMembers().size() + ") from party",
                            ChatColor.RED,
                            "Kick all the players who are already in games from the party",
                            "party kick-busy"
                    )
                    .send(party.getOwner());

            return;
        }

        Set<MiniGameController> games = findGames(game);

        MiniGameController selectedGame = null;

        for(MiniGameController eachGame : games)
            if(eachGame.getOptions().getMaxPlayers() - eachGame.getPlayers().size() >= party.getMembers().size()) {
                selectedGame = eachGame;
                break;
            }

        if(selectedGame == null)
            selectedGame = createNewGame(game);

        assert selectedGame != null;

        selectedGame.addParty(party);
    }

    public static MiniGameController getGameFromId(String id) {
        for(MiniGameController eachGame : registeredGames)
            if(eachGame.getGameId().equals(id))
                return eachGame;

        return null;
    }

    public static MiniGameController createNewGame(GameDefinition game) {
        if(registeredGames.size() >= 100) {
            new WarningMessage("Reached mini-game limit! 100 mini-games are running right now. This is the current limit, and new games cannot be created before old ones stop.").broadcast();
            return null;
        }

        return game.createGame();
    }

    public static int countPlayersInGame(GameDefinition game) {
        int count = 0;

        for(MiniGameController eachMiniGame : findGames(game))
            count += eachMiniGame.getPlayers().size();

        return count;
    }
}
