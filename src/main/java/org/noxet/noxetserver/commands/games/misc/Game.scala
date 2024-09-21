package org.noxet.noxetserver.commands.games.misc

import org.bukkit.command.{Command, CommandSender, TabExecutor}
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import org.noxet.noxetserver.NoxetServer
import org.noxet.noxetserver.commands.RegisteredCommand
import org.noxet.noxetserver.messaging.ErrorMessage.ErrorType.*
import org.noxet.noxetserver.messaging.{ErrorMessage, Message, SuccessMessage}
import org.noxet.noxetserver.minigames.party.Party
import org.noxet.noxetserver.minigames.worldeater.WorldEater
import org.noxet.noxetserver.minigames.{GameDefinition, MiniGameController, MiniGameManager}

object Game extends TabExecutor, RegisteredCommand("game", this):
  override def onCommand(commandSender: CommandSender, command: Command, s: String, strings: Array[String]): Boolean =
    strings match
      case Array() =>
        ErrorMessage(Argument, "Missing argument: what way to manage mini-games.").send(commandSender)
        false
      case Array("play") =>
        ErrorMessage(Argument, "Missing argument: what game to play.").send(player)
        true
      case Array("play", gameName) =>
        commandSender match
          case p: Player if MiniGameManager.isPlayerBusyInGame(p) =>
            ErrorMessage(Common, "You are already in a game.").send(player)
          case p: Player =>
            GameDefinition.values.find(_.options.getId == gameName) match
              case Some(gameToPlay) => MiniGameManager.playGame(p, gameToPlay)
              case None => ErrorMessage(Argument, s"No mini-game by such name: '$gameName'.").send(p)
          case _ =>
            ErrorMessage(Common, "Only players can join games.").send(commandSender)
        true
      case Array("join") =>
        ErrorMessage(Argument, "Missing argument: ID of game to join.").send(player)
        true
      case Array("join", gameId) =>
        commandSender match
          case p: Player if Party.isPlayerMemberOfParty(p) =>
            ErrorMessage(Common, "You are in a party, and cannot join games alone.").send(p)
          case p: Player if MiniGameManager.isPlayerBusyInGame(p) =>
            ErrorMessage(Common, "You are already in a game.").send(player)
          case p: Player =>
            MiniGameManager.getGameFromId(gameId) match
              case Some(game) => game.addPlayer(p)
              case None => ErrorMessage(Common, "No game with such ID. It may have ended.").send(p)
          case _ =>
            ErrorMessage(Common, "Only players can join games.").send(commandSender)
        true
      case Array("leave") =>
        commandSender match
          case p: Player =>
            MiniGameManager.findPlayersOrSpectatorsGame(player) match
              case Some(game) =>
                game.disbandPlayer(p) // Try to remove both as player and spectator.
                Message("§cYou left the game.").send(p)
              case None =>
                ErrorMessage(Common, "You are not in a game.").send(p)
          case _ =>
            ErrorMessage(Common, "Only players can leave games.").send(commandSender)
        true
      case Array("spectate") =>
        ErrorMessage(Argument, "Missing argument: game ID to spectate.").send(player)
        true
      case Array("spectate", gameId) =>
        commmandSender match
          case p: Player if MiniGameManager.isPlayerBusyInGame(p) =>
            ErrorMessage(Common, "You are already in a game.")
              .addButton(
                "Leave",
                ChatColor.RED,
                "Leave this game",
                "game leave")
              .send(p)
          case p: Player =>
            MiniGameManager.getGameFromId(gameId) match
              case Some(game) => game.addSpectator(player)
              case None =>
                ErrorMessage(Common, "Invalid game ID. Is it already over?").send(p)
          case _ =>
            ErrorMessage(Common, "Only players can spectate games.").send(commandSender)
        true
      case Array("info") =>
        commandSender match
          case p: Player if MiniGameManager.findPlayersOrSpectatorsGame(p).isDefined =>
            val game = MiniGameManager.findPlayersOrSpectatorsGame(p).get
            p.performCommand(s"game info ${game.gameId}")
          case _ =>
            ErrorMessage(Argument,
              "Unless you are in a game you must provide an argument " +
                "for the game ID to see info about.").send(commandSender)
        true
      case Array("info", gameId) =>
        MiniGameManager.getGameFromId(gameId) match
          case Some(game) =>
            Message(
              Map(
                "Game ID" -> game.gameId,
                "Game" -> game.game.options.getDisplayName,
                "Status" -> game.state,
                "Players" -> game.getPlayers.size + " / " + game.game.options.getMaxPlayers,
                "Spectators" -> game.getSpectators.size,
              ).map((k, v) => s"§3$k: §b$v")
            ).addButton(
              "Join",
              ChatColor.GREEN,
              "Join this game",
              s"game join ${game.gameId}"
            ).addButton(
              "Spectate",
              ChatColor.GOLD,
              "Spectate this game",
              s"game spectate ${game.gameId}"
            ).addButton(
              "Stop",
              ChatColor.RED,
              "Soft stop this game",
              s"game stop soft ${game.gameId}"
            ).addButton(
              "Hard-stop",
              ChatColor.DARK_RED,
              "Hard stop this game (instant stop)",
              s"game stop hard ${game.gameId}"
            ).send(commandSender)
          case None =>
            ErrorMessage(Common, "Invalid game ID.").send(commandSender)
        true
      case Array("list") =>
        Message(s"§3Running games: ${MiniGameManager.getRegisteredGames.size}").send(commandSender)

        for game <- MiniGameManager.getRegisteredGames do
          Message(
            s"└§3§l${game.game.options.getDisplayName} §b${game.gameId}"
          ).addButton(
            "Info",
            ChatColor.YELLOW,
            "Read more about this game",
            s"game info ${game.gameId}"
          ).send(commandSender)

        true
      case Array("stop", _*) if !commandSender.isOp =>
        ErrorMessage(Permission, "Only operators can stop games.").send(commandSender)
        true
      case Array("stop") =>
        ErrorMessage(Argument, "Missing argument: stop mode (soft/hard).").send(commandSender)
        true
      case Array("stop", mode) =>
        commandSender match
          case p: Player if MiniGameManager.findPlayersOrSpectatorsGame(p) =>
            val game = MiniGameManager.findPlayersOrSpectatorsGame(p).get
            p.performCommand(s"game stop ${game.gameId}")
          case _ =>
            ErrorMessage(Common, "Missing argument: game ID to stop. " +
              "In-game players/spectators can omit this argument.").send(commandSender)
        true
      case Array("stop", mode, "*") =>
        MiniGameManager.getRegisteredGames.foreach(g =>
          commandSender.performCommand(s"game stop $mode ${g.gameId}"))
      case Array("stop", mode, gameId) =>
        hard = mode match
          case "hard" => true
          case "soft" => false
          case _ =>
            ErrorMessage(Argument, "Invalid argument: stop mode (soft/hard).")
              .send(commandSender)
            return true

        MiniGameManager.getGameFromId(gameId) match
          case Some(game) =>
            Message("§cStopping game " +
              (if hard then "hard (instantly)"
              else "soft (waiting for handler)") +
              "...")
              .send(commandSender)

            val ticks =
              if hard then
                game.stop()
                0
              else
                game.softStop

            if ticks != 0 then
              Message(s"§4Soft stop margin: ${ticks / 20} seconds.").send(commandSender)

            game.scheduleTask(() =>
              SuccessMessage(s"Game ${game.gameId} stopped.").send(commandSender),
              Math.max(ticks - 1, 0))
          case None =>
            ErrorMessage(Common, "Invalid game ID.").send(commandSender)
        true
      case Array("debug-create") =>
        ErrorMessage(Argument, "Missing argument: what game to play.").send(player)
        true
      case Array("debug-create", gameName) =>
        commandSender match
          case s if !s.isOp =>
            ErrorMessage(Permission, "Only operators can create games.").send(commandSender)
          case p: Player if MiniGameManager.isPlayerBusyInGame(player) =>
            ErrorMessage(Common, "You are already in a game.").send(player)
          case p: Player =>
            GameDefinition.values.find(_.options.getId == gameName) match
              case Some(gameDef) =>
                val game = MiniGameManager.createNewGame(gameToPlay)
                game.addPlayer(player)
                // start() does not check for minimum player requirement, so the game will start anyway.
                game.start()
              case None =>
                ErrorMessage(Argument, "That is not a mini-game.").send(player)
          case _ =>
            ErrorMessage(Common, "Only players can create games.").send(commandSender)
        true
      case Array(subCommand, _*) =>
        ErrorMessage(Argument, s"Invalid subcommand: '${subCommand}'.")
          .send(commandSender)
        false

  override def onTabComplete(commandSender: CommandSender, command: Command, s: String, strings: Array[String]): List[String] =
    strings match
      case Array(_) =>
        List("play", "join", "leave", "spectate", "info", "list", "stop", "debug-create")
      case Array(subCommand, _) =>
        subCommand match
          case "play" | "debug-create" =>
            GameDefinition.values.map(_.options.getId)
          case "stop" =>
            List("soft", "hard")
      case Array(subCommand, _, _) =>
        subCommand match
          case "stop" => List("*")
          case _ => List()