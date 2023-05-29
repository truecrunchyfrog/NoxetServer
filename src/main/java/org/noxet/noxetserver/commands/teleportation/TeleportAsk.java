package org.noxet.noxetserver.commands.teleportation;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.noxet.noxetserver.NoxetServer;
import org.noxet.noxetserver.RealmManager;
import org.noxet.noxetserver.messaging.NoxetErrorMessage;
import org.noxet.noxetserver.messaging.NoxetMessage;

import java.util.*;

@SuppressWarnings("ALL")
public class TeleportAsk implements TabExecutor {

    // Key: requester
    // Value: target
    private static final Map<Player, Player> requests = new HashMap<>();

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(!(commandSender instanceof Player)) {
            new NoxetErrorMessage("Only players can make teleportation requests.").send(commandSender);
            return true;
        }

        Player player = (Player) commandSender;

        RealmManager.Realm realm = RealmManager.getCurrentRealm(player);

        if(realm == null) {
            new NoxetErrorMessage("You must be in a realm to do this.").send(player);
            return true;
        }

        if(strings.length == 0) {
            new NoxetErrorMessage("Missing argument for player to send the request to (or cancel/accept/deny).").send(player);
            return true;
        }

        if(strings[0].equalsIgnoreCase("cancel")) {
            if(!requests.containsKey(player)) {
                new NoxetErrorMessage("You have not sent any request.").send(player);
                return true;
            }

            Player targetPlayer = requests.get(player);

            new NoxetMessage("§cYou aborted your teleport request to " + targetPlayer.getDisplayName() + ".").send(player);

            new NoxetMessage("§c" + player.getDisplayName() + " aborted their teleportation request to you.").send(targetPlayer);

            requests.remove(player);

            return true;
        }

        if(strings[0].equalsIgnoreCase("accept")) {
            if(!requests.containsValue(player)) {
                new NoxetErrorMessage("You have not received a teleportation request.").send(player);
                return true;
            }

            Player specificPlayer = null;

            if(strings.length > 1) {
                if(Objects.equals(strings[1], "*")) {
                    new NoxetMessage("§eAccepting all incoming requests...").send(player);

                    for(Map.Entry<Player, Player> request : requests.entrySet())
                        if(request.getValue() == player) {
                            player.performCommand("tpa accept " + request.getKey().getName());
                        }

                    return true;
                }


                specificPlayer = NoxetServer.getPlugin().getServer().getPlayer(strings[1]);

                if(specificPlayer == null) {
                    new NoxetErrorMessage("That player is not present on Noxet.org right now.").send(player);
                    return true;
                }
            }

            Player requester = null;

            if(specificPlayer == null) {
                for(Map.Entry<Player, Player> request : requests.entrySet())
                    if(request.getValue() == player) {
                        requester = request.getKey();
                        break;
                    }
            } else {
                if(!requests.containsKey(specificPlayer)) {
                    new NoxetErrorMessage(specificPlayer.getDisplayName() + " has not sent a teleportation request to you.").send(player);
                    return true;
                }

                requester = specificPlayer;
            }

            assert requester != null;

            new NoxetMessage("§eAccepting request...").send(player);
            new NoxetMessage("§e" + player.getDisplayName() + " accepted your teleportation request.").send(requester);

            requester.teleport(player);

            new NoxetMessage("§e" + requester.getDisplayName() + " has teleported to you.").send(player);
            new NoxetMessage("§eYou have been teleported.").send(requester);

            requests.remove(requester);

            return true;
        }

        if(strings[0].equalsIgnoreCase("deny")) {
            if(!requests.containsValue(player)) {
                new NoxetErrorMessage("You have not received a teleportation request.").send(player);
                return true;
            }

            Player specificPlayer = null;

            if(strings.length > 1) {
                if(Objects.equals(strings[1], "*")) {
                    new NoxetMessage("§eDenying all incoming requests...").send(player);

                    for(Map.Entry<Player, Player> request : requests.entrySet())
                        if(request.getValue() == player) {
                            player.performCommand("tpa deny " + request.getKey().getName());
                        }

                    return true;
                }

                specificPlayer = NoxetServer.getPlugin().getServer().getPlayer(strings[1]);

                if(specificPlayer == null) {
                    new NoxetErrorMessage("That player is not present on Noxet.org right now.").send(player);
                    return true;
                }
            }

            Player requester = null;

            if(specificPlayer == null) {
                for(Map.Entry<Player, Player> request : requests.entrySet())
                    if(request.getValue() == player) {
                        requester = request.getKey();
                        break;
                    }
            } else {
                if(!requests.containsKey(specificPlayer)) {
                    new NoxetErrorMessage(specificPlayer.getDisplayName() + " has not sent a teleportation request to you.").send(player);
                    return true;
                }

                requester = specificPlayer;
            }

            assert requester != null;

            requests.remove(requester);

            new NoxetMessage("§eYou denied the teleportation request from " + requester.getDisplayName() + ".").send(player);
            new NoxetMessage("§e" + player.getDisplayName() + " denied your teleportation request.").send(requester);

            return true;
        }

        if(requests.containsKey(player)) {
            new NoxetErrorMessage("You have already made a teleportation request. Cancel it before you can make another one.").send(player);
            return true;
        }

        Player targetPlayer = NoxetServer.getPlugin().getServer().getPlayer(strings[0]);

        if(targetPlayer == null) {
            new NoxetErrorMessage("That player is not present on Noxet.org right now.").send(player);
            return true;
        }

        if(targetPlayer == player) {
            new NoxetErrorMessage("You cannot teleport to yourself!").send(player);
            return true;
        }

        RealmManager.Realm targetRealm = RealmManager.getCurrentRealm(targetPlayer);

        if(targetRealm == null) {
            new NoxetErrorMessage(targetPlayer.getDisplayName() + " is not in a realm.").send(player);
            return true;
        }

        if(realm != targetRealm) {
            new NoxetErrorMessage(targetPlayer.getDisplayName() + " is not in " + realm.getDisplayName() + ". They are in " + targetRealm.getDisplayName() + "! You must move to that realm first.").send(player);
            return true;
        }

        requests.put(player, targetPlayer);

        new NoxetMessage("§eSent a teleportation request to " + targetPlayer.getDisplayName() + ".").send(player);
        new NoxetMessage().addButton("Cancel", ChatColor.RED, "Cancel your request", "tpa cancel").send(player);

        new NoxetMessage("§6" + player.getDisplayName() + "§e has sent you a teleportation request.").send(targetPlayer);
        new NoxetMessage()
                .addButton(
                        "Accept",
                        ChatColor.GREEN,
                        "Teleport " + player.getName() + " to you",
                        "tpa accept " + player.getName())
                .addButton(
                        "Deny",
                        ChatColor.RED,
                        "Deny this request",
                        "tpa deny " + player.getName())
                .send(targetPlayer);

        new BukkitRunnable() {
            @Override
            public void run() {
                if(requests.remove(player, targetPlayer)) { // If value was removed; was still active.
                    // Meaning that the request was still valid. But now it has expired.
                    new NoxetMessage("§cYour teleportation request to " + targetPlayer.getDisplayName() + " has expired.").send(player);
                }
            }
        }.runTaskLater(NoxetServer.getPlugin(), 20 * 60);

        return true;
    }

    public static void abortPlayerRelatedRequests(Player player) {
        if(requests.containsKey(player)) {
            new NoxetMessage("§cThe teleportation request from " + player.getDisplayName() + " has been aborted because they left the realm.").send(requests.get(player));
            requests.remove(player); // Remove request FROM player.
        }

        for(Map.Entry<Player, Player> request : requests.entrySet())
            if(request.getValue() == player) {
                new NoxetMessage("§cYour teleportation request to " + player.getDisplayName() + " has been aborted because they left the realm.").send(request.getKey());
                requests.remove(request.getKey()); // Remove requests TO player.
            }
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        if(!(commandSender instanceof Player))
            return null;

        Player player = (Player) commandSender;

        List<String> completions = new ArrayList<>();

        if(strings.length == 1) {
            completions.addAll(Arrays.asList("cancel", "accept", "deny"));

            RealmManager.Realm realm = RealmManager.getCurrentRealm(player);

            if(realm != null)
                for(Player playerInRealm : realm.getPlayers())
                    if(playerInRealm != player)
                        completions.add(playerInRealm.getName());
        } else if(strings.length == 2) {
            switch(strings[1].toLowerCase()) {
                case "accept":
                case "deny":
                    completions.add("*");

                    for(Map.Entry<Player, Player> request : requests.entrySet())
                        if(request.getValue() == player)
                            completions.add(request.getKey().getName());

                    break;
            }
        }

        return completions;
    }
}
