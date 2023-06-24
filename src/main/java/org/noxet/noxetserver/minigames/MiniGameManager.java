package org.noxet.noxetserver.minigames;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.noxet.noxetserver.messaging.WarningMessage;

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

    public static boolean isPlayerBusyInGame(Player player) {
        MiniGameController playersGame = findPlayersGame(player);

        return playersGame != null && !playersGame.hasEnded();
    }

    /**
     * Find the game that is running on a world. Players in a game may not actually be in the game's world (when game is stalling), so be careful with this.
     * @param world The world to search for
     * @return The game instance that has the world if found, otherwise null
     */
    public static MiniGameController findWorldsGame(World world) {
        for(MiniGameController eachGame : registeredGames)
            if(eachGame.getWorkingWorld() == world)
                return eachGame;
        return null;
    }

    public static boolean joinGame(Player player, GameDefinition game) {
        Set<MiniGameController> games = findGames(game);

        MiniGameController selectedGame = null;

        for(MiniGameController eachGame : games)
            if(!eachGame.isFull() && (selectedGame == null ||
                    (
                            (selectedGame.enoughPlayers() &&
                            !eachGame.enoughPlayers()) || // Prioritize this game, with not enough players.
                            (eachGame.getPlayers().size() > selectedGame.getPlayers().size()) // Prioritize filling up this game before others.
                    ))
            )
                selectedGame = eachGame;

        if(selectedGame == null)
            return false;

        return selectedGame.addPlayer(player);
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
}
