package org.noxet.noxetserver.commands.games.misc;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.noxet.noxetserver.NoxetServer;
import org.noxet.noxetserver.messaging.ErrorMessage;
import org.noxet.noxetserver.messaging.Message;
import org.noxet.noxetserver.messaging.SuccessMessage;
import org.noxet.noxetserver.minigames.GameDefinition;
import org.noxet.noxetserver.minigames.MiniGameController;
import org.noxet.noxetserver.minigames.MiniGameManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class Game implements TabExecutor {
    @Override
    @SuppressWarnings("NullableProblems")
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(strings.length == 0) {
            new ErrorMessage(ErrorMessage.ErrorType.ARGUMENT, "Missing argument: what way to manage mini-games.").send(commandSender);
            return false;
        }

        if(strings[0].equalsIgnoreCase("play")) {
            if(!(commandSender instanceof Player)) {
                new ErrorMessage(ErrorMessage.ErrorType.COMMON, "Only players can join games.").send(commandSender);
                return true;
            }

            Player player = (Player) commandSender;

            if(strings.length < 2) {
                new ErrorMessage(ErrorMessage.ErrorType.ARGUMENT, "Missing argument: what game to play.").send(player);
                return true;
            }

            GameDefinition gameToPlay = null;

            for(GameDefinition gameDefinition : GameDefinition.values())
                if(gameDefinition.getOptions().getId().equals(strings[1])) {
                    gameToPlay = gameDefinition;
                    break;
                }

            if(gameToPlay == null) {
                new ErrorMessage(ErrorMessage.ErrorType.ARGUMENT, "That is not a mini-game.").send(player);
                return true;
            }

            if(MiniGameManager.isPlayerBusyInGame(player)) {
                new ErrorMessage(ErrorMessage.ErrorType.COMMON, "You are already in a game.").send(player);
                return true;
            }

            if(!MiniGameManager.joinGame(player, gameToPlay))
                Objects.requireNonNull(MiniGameManager.createNewGame(gameToPlay)).addPlayer(player);

            return true;
        } else if(strings[0].equalsIgnoreCase("join")) {
            if(!(commandSender instanceof Player)) {
                new ErrorMessage(ErrorMessage.ErrorType.COMMON, "Only players can join games.").send(commandSender);
                return true;
            }

            Player player = (Player) commandSender;

            if(strings.length < 2) {
                new ErrorMessage(ErrorMessage.ErrorType.ARGUMENT, "Missing argument: ID of game to join.").send(player);
                return true;
            }

            MiniGameController game = MiniGameManager.getGameFromId(strings[1]);

            if(game == null) {
                new ErrorMessage(ErrorMessage.ErrorType.COMMON, "There is no game by such ID. It may have ended.").send(player);
                return true;
            }

            if(MiniGameManager.isPlayerBusyInGame(player)) {
                new ErrorMessage(ErrorMessage.ErrorType.COMMON, "You are already in a game.").send(player);
                return true;
            }

            game.addPlayer(player);

            return true;
        } else if(strings[0].equalsIgnoreCase("leave")) {
            if(!(commandSender instanceof Player)) {
                new ErrorMessage(ErrorMessage.ErrorType.COMMON, "Only players can leave games.").send(commandSender);
                return true;
            }

            Player player = (Player) commandSender;

            MiniGameController game = MiniGameManager.findPlayersOrSpectatorsGame(player);

            if(game == null) {
                new ErrorMessage(ErrorMessage.ErrorType.COMMON, "You are not in a game.").send(player);
                return true;
            }

            game.disbandPlayer(player); // Try to remove both as player and spectator.

            return true;
        } else if(strings[0].equalsIgnoreCase("spectate")) {
            if(!(commandSender instanceof Player)) {
                new ErrorMessage(ErrorMessage.ErrorType.COMMON, "Only players can spectate games.").send(commandSender);
                return true;
            }

            Player player = (Player) commandSender;

            if(strings.length < 2) {
                new ErrorMessage(ErrorMessage.ErrorType.ARGUMENT, "Missing argument: game ID to spectate.").send(player);
                return true;
            }

            MiniGameController game = MiniGameManager.getGameFromId(strings[1]);

            if(game == null) {
                new ErrorMessage(ErrorMessage.ErrorType.COMMON, "Invalid game ID. Is it already over?").send(player);
                return true;
            }

            if(MiniGameManager.isPlayerBusyInGame(player)) {
                new ErrorMessage(ErrorMessage.ErrorType.COMMON, "You are already in a game.").addButton("Leave", ChatColor.RED, "Leave this game", "game leave").send(player);
                return true;
            }

            game.addSpectator(player);

            return true;
        } else if(strings[0].equalsIgnoreCase("info")) {
            if(!(commandSender instanceof Player) && strings.length < 2) {
                new ErrorMessage(ErrorMessage.ErrorType.ARGUMENT, "Non-players must provide an argument for the game ID to see info about.").send(commandSender);
                return true;
            }

            MiniGameController game = strings.length > 1 ? MiniGameManager.getGameFromId(strings[1]) : MiniGameManager.findPlayersOrSpectatorsGame((Player) commandSender);

            if(game == null) {
                if(strings.length > 1) {
                    new ErrorMessage(ErrorMessage.ErrorType.COMMON, "Invalid game ID.").send(commandSender);
                    return true;
                }

                new ErrorMessage(ErrorMessage.ErrorType.COMMON, "You are not in a game. To see info about a game, you must either be in the game or provide the game ID as an argument.").send(commandSender);
                return true;
            }

            new Message(
                    "§3Game ID: §b" + game.getGameId() + "\n" +
                    "§3Game: §b" + game.getGame().getOptions().getDisplayName() + "\n" +
                    "§3Status: §b" + game.getState() + "\n" +
                    "§3Players: §b" + game.getPlayers().size() + " / " + game.getGame().getOptions().getMaxPlayers() + "\n" +
                    "§3Spectators: §b" + game.getSpectators().size() + "\n"
            ).addButton(
                    "Join",
                    ChatColor.GREEN,
                    "Join this game",
                    "game join " + game.getGameId()
            ).addButton(
                    "Spectate",
                    ChatColor.GOLD,
                    "Spectate this game",
                    "game spectate " + game.getGame()
            ).addButton(
                    "Stop",
                    ChatColor.RED,
                    "Soft stop this game",
                    "game stop soft " + game.getGameId()
            ).addButton(
                    "Hard-stop",
                    ChatColor.DARK_RED,
                    "Hard stop this game (instant stop)",
                    "game stop hard " + game.getGameId()
            ).send(commandSender);

            return true;
        } else if(strings[0].equalsIgnoreCase("list")) {
            new Message("§3Running games: " + MiniGameManager.getRegisteredGames().size()).send(commandSender);

            for(MiniGameController game : MiniGameManager.getRegisteredGames())
                new Message(
                        "└§3§l" + game.getGame().getOptions().getDisplayName() + " §b" + game.getGameId()
                ).addButton(
                        "Info",
                        ChatColor.YELLOW,
                        "Read more about this game",
                        "game info " + game.getGameId()
                ).send(commandSender);

            return true;
        } else if(strings[0].equalsIgnoreCase("stop")) {
            if(!commandSender.isOp()) {
                new ErrorMessage(ErrorMessage.ErrorType.PERMISSION, "Only operators can stop games.").send(commandSender);
                return true;
            }

            if(strings.length < 2) {
                new ErrorMessage(ErrorMessage.ErrorType.ARGUMENT, "Missing argument: stop mode (soft/hard).").send(commandSender);
                return true;
            }

            boolean hard;

            switch(strings[1].toLowerCase()) {
                case "hard":
                    hard = true;
                    break;
                case "soft":
                    hard = false;
                    break;
                default:
                    new ErrorMessage(ErrorMessage.ErrorType.ARGUMENT, "Invalid argument: stop mode (soft/hard).").send(commandSender);
                    return true;
            }

            if(strings.length < 3) {
                new ErrorMessage(ErrorMessage.ErrorType.ARGUMENT, "Missing argument: game ID to stop.").send(commandSender);
                return true;
            }

            if(strings[2].equals("*")) {
                new Message("Stopping all games " + (hard ? "hard" : "soft") + "... (" + MiniGameManager.getRegisteredGames().size() + ")").send(commandSender);

                int maxTicks = 0;

                for(MiniGameController game : MiniGameManager.getRegisteredGames())
                    if(hard)
                        game.stop();
                    else
                        maxTicks = Math.max(game.softStop(), maxTicks);

                if(!hard)
                    new Message("§4All games will have been stopped in " + maxTicks / 20 + " seconds.").send(commandSender);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        new SuccessMessage("All games have been stopped.").send(commandSender);
                    }
                }.runTaskLater(NoxetServer.getPlugin(), maxTicks);

                return true;
            }

            MiniGameController game = MiniGameManager.getGameFromId(strings[2]);

            if(game == null) {
                new ErrorMessage(ErrorMessage.ErrorType.COMMON, "Invalid game ID.").send(commandSender);
                return true;
            }

            new Message("§cStopping game " + (hard ? "hard (instantly)" : "soft (waiting for handler)") + "...").send(commandSender);

            int ticks;

            if(hard) {
                ticks = 0;
                game.stop();
            } else {
                ticks = game.softStop();
                new Message("§4Soft stop margin: " + ticks / 20 + " seconds.").send(commandSender);
            }

            new BukkitRunnable() {
                @Override
                public void run() {
                    new SuccessMessage("Game " + game.getGameId() + " stopped.").send(commandSender);
                }
            }.runTaskLater(NoxetServer.getPlugin(), ticks);

            return true;
        } else {
            new ErrorMessage(ErrorMessage.ErrorType.ARGUMENT, "Invalid subcommand: '" + strings[0] + "'.").send(commandSender);
            return false;
        }
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        List<String> completions = new ArrayList<>();

        if(strings.length == 1) {
            completions.addAll(Arrays.asList("play", "join", "leave", "spectate", "info", "list", "stop"));
        } else if(strings.length == 2) {
            switch(strings[0].toLowerCase()) {
                case "play":
                    for(GameDefinition eachGameDefinition : GameDefinition.values())
                        completions.add(eachGameDefinition.getOptions().getId());
                    break;
                case "stop":
                    completions.addAll(Arrays.asList("soft", "hard"));
                    break;
            }
        } else if(strings.length == 3) {
            if(strings[0].equalsIgnoreCase("stop"))
                completions.add("*");
        }

        return completions;
    }
}
