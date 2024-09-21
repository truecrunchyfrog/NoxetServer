package org.noxet.noxetserver.minigames

import org.bukkit.entity.Player
import org.noxet.noxetserver.messaging.{ErrorMessage, WarningMessage}
import org.noxet.noxetserver.minigames.party.Party

object MiniGameManager:
  private val registeredGames: Set[MiniGameController] = HashSet()

  def registerGame(miniGame: MiniGameController): Unit = registeredGames.add(miniGame)

  def unregisterGame(miniGame: MiniGameController): Unit = registeredGames.remove(miniGame)

  def getRegisteredGames: Set[MiniGameController] = registeredGames

  def findGames(gameDefinition: GameDefinition): Set[MiniGameController] =
    registeredGames.filter(_.game == gameDefinition)

  def findPlayersGame(player: Player): Option[MiniGameController] =
    registeredGames.find(_.isPlayer(player))

  def findSpectatorsGame(player: Player): Option[MiniGameController] =
    registeredGames.find(_.isSpectator(player))

  def findPlayersOrSpectatorsGame(player: Player): Option[MiniGameController] =
    registeredGames.find(_.getPlayersAndSpectators.contains(player))

  /**
   * Check if a player is currently in a game that has not ended.
   * This will only check if the player is participating, not spectating!
   *
   * @param player the player to check for.
   * @return `true` if the player is currently participating in a game that has not yet ended, otherwise `false`.
   */
  def isPlayerBusyInGame(player: Player): Boolean =
    registeredGames.exists(game => game.isPlayer(player) && !game.hasEnded)

  /**
   * Sort a list of games descendingly.
   * Firstly by if the game has enough players (prefer not enough).
   * Secondly by size (prefer with more players, to fill them up faster).
   * Thirdly by age (prefer older lobbies, where the players have waited longer).
   *
   * @param games the games to be sorted.
   * @return the games, sorted for a good queue priority when finding available games.
   */
  def sortGames(games: Seq[MiniGameController]): Seq[MiniGameController] =
    games.sortBy(g => (!g.enoughPlayers, g.getPlayers.size, g.createdAt))(_ > _)

  def playGame(player: Player, game: GameDefinition): Unit =
    Party.getPartyFromMember(player) match
      case Some(party) =>
        if !party.isOwner(player) then
          ErrorMessage(
            ErrorMessage.ErrorType.Common,
            "You are in a party. Only the party owner can join games for the party.\n" +
              "You can leave the party to join by yourself, " +
              "or have the owner transfer the ownership to you to start games yourself.")
            .send(player)
          return

        partyPlayGame(party, game)
        return
      case None => ()

    sortGames(findGames(game).filter(_.canJoin(1)))
      .headOption.getOrElse(createNewGame(game))
      .addPlayer(player)

  def partyPlayGame(party: Party, game: GameDefinition): Unit =
    if game.options.getMaxPlayers < party.getMembers.size then
      party.sendPartyMessage(
        ErrorMessage(ErrorMessage.ErrorType.Common,
          s"Tried to join a game of ${game.options.getDisplayName}, " +
            "but there are too many players in this party. " +
            s"The game can only allow ${game.options.getMaxPlayers} players, " +
            s"but there are ${party.getMembers.size} players in this party."))
      return

    if !party.isPartyReadyForGame then
      WarningMessage("Cannot join game, because all members may not be ready.")
        .addButton(
          s"Kick busy players (${party.getBusyMembers.size}) from party",
          ChatColor.RED,
          "Kick all the players who are already in games from the party",
          "party kick-busy"
        ).send(party.owner)
      return

    sortGames(findGames(game).filter(_.canJoin(party.getMembers.size)))
      .headOption.getOrElse(createNewGame(game))
      .addParty(party)

  def getGameFromId(id: String): Option[MiniGameController] =
    registeredGames.find(_.gameId == id)

  def createNewGame(game: GameDefinition): MiniGameController =
    if registeredGames.size >= 100 then
      // TODO better guard solution
      WarningMessage(
        "Reached mini-game limit! 100 mini-games are running right now. " +
          "This is the current limit, " +
          "and new games cannot be created before old ones stop.")
        .broadcast()
      return null

    game.createGame

  def countPlayersInGame(game: GameDefinition): Int =
    findGames(game).flatMap(_.getPlayers.size).sum