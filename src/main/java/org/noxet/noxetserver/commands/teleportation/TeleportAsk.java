package org.noxet.noxetserver.commands.teleportation;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Particle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.noxet.noxetserver.Events;
import org.noxet.noxetserver.NoxetServer;
import org.noxet.noxetserver.realm.RealmManager;
import org.noxet.noxetserver.util.UsernameStorageManager;
import org.noxet.noxetserver.commands.social.Friend;
import org.noxet.noxetserver.messaging.ErrorMessage;
import org.noxet.noxetserver.messaging.Message;
import org.noxet.noxetserver.messaging.NoteMessage;
import org.noxet.noxetserver.messaging.SuccessMessage;
import org.noxet.noxetserver.playerdata.PlayerDataManager;
import org.noxet.noxetserver.util.TeleportUtil;

import java.util.*;

@SuppressWarnings("ALL")
public class TeleportAsk implements TabExecutor {

    // Key: requester
    // Value: target
    private static final Map<Player, Player> requests = new HashMap<>();

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(!(commandSender instanceof Player)) {
            new ErrorMessage(ErrorMessage.ErrorType.COMMON, "Only players can make teleportation requests.").send(commandSender);
            return true;
        }

        Player player = (Player) commandSender;

        RealmManager.Realm realm = RealmManager.getCurrentRealm(player);

        if(realm == null) {
            new ErrorMessage(ErrorMessage.ErrorType.COMMON, "You must be in a realm to do this.").send(player);
            return true;
        }

        if(!realm.doesAllowTeleportationMethods()) {
            new ErrorMessage(ErrorMessage.ErrorType.COMMON, "This realm does not allow TPA.").send(player);
            return true;
        }

        if(strings.length == 0) {
            new ErrorMessage(ErrorMessage.ErrorType.ARGUMENT, "Missing argument for player to send the request to (or cancel/accept/deny).").send(player);
            return true;
        }

        if(strings[0].equalsIgnoreCase("cancel")) {
            if(!requests.containsKey(player)) {
                new ErrorMessage(ErrorMessage.ErrorType.COMMON, "You have not sent any request.").send(player);
                return true;
            }

            Player targetPlayer = requests.get(player);

            new Message("§cYou aborted your teleport request to " + targetPlayer.getName() + ".").send(player);

            new Message("§c" + player.getName() + " aborted their teleportation request to you.").send(targetPlayer);

            requests.remove(player);

            return true;
        }

        if(strings[0].equalsIgnoreCase("accept")) {
            if(!requests.containsValue(player)) {
                new ErrorMessage(ErrorMessage.ErrorType.COMMON, "You have not received a teleportation request.").send(player);
                return true;
            }

            Player specificPlayer = null;

            if(strings.length > 1) {
                if(Objects.equals(strings[1], "*")) {
                    new Message("§eAccepting all incoming requests...").send(player);

                    for(Map.Entry<Player, Player> request : requests.entrySet())
                        if(request.getValue() == player)
                            player.performCommand("tpa accept " + request.getKey().getName());

                    return true;
                }


                specificPlayer = NoxetServer.getPlugin().getServer().getPlayer(strings[1]);

                if(specificPlayer == null) {
                    new ErrorMessage(ErrorMessage.ErrorType.COMMON, "That player is not present on Noxet.org right now.").send(player);
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
                    new ErrorMessage(ErrorMessage.ErrorType.COMMON, specificPlayer.getName() + " has not sent a teleportation request to you.").send(player);
                    return true;
                }

                requester = specificPlayer;
            }

            assert requester != null;

            new Message("§eAccepting request...").send(player);
            new Message("§e" + player.getName() + " accepted your teleportation request.").send(requester);

            if(!TeleportUtil.isLocationTeleportSafe(player.getLocation())) {
                new ErrorMessage(ErrorMessage.ErrorType.COMMON, "We blocked this teleportation because it appears to be unsafe. " + player.getName() + ", please move to a solid block and accept from there.").send(Arrays.asList(player, requester));
                return true;
            }

            requests.remove(requester);

            Events.setTemporaryInvulnerability(player);
            requester.teleport(player);
            requester.getWorld().spawnParticle(Particle.DRAGON_BREATH, requester.getLocation(), 56);

            new Message("§e" + requester.getName() + " has teleported to you.").send(player);
            new Message("§eYou have been teleported.").send(requester);

            return true;
        }

        if(strings[0].equalsIgnoreCase("deny")) {
            if(!requests.containsValue(player)) {
                new ErrorMessage(ErrorMessage.ErrorType.COMMON, "You have not received a teleportation request.").send(player);
                return true;
            }

            Player specificPlayer = null;

            if(strings.length > 1) {
                if(Objects.equals(strings[1], "*")) {
                    new Message("§eDenying all incoming requests...").send(player);

                    for(Map.Entry<Player, Player> request : requests.entrySet())
                        if(request.getValue() == player) {
                            player.performCommand("tpa deny " + request.getKey().getName());
                        }

                    return true;
                }

                specificPlayer = NoxetServer.getPlugin().getServer().getPlayer(strings[1]);

                if(specificPlayer == null) {
                    new ErrorMessage(ErrorMessage.ErrorType.COMMON, "That player is not present on Noxet.org right now.").send(player);
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
                    new ErrorMessage(ErrorMessage.ErrorType.COMMON, specificPlayer.getName() + " has not sent a teleportation request to you.").send(player);
                    return true;
                }

                requester = specificPlayer;
            }

            assert requester != null;

            requests.remove(requester);

            new Message("§eYou denied the teleportation request from " + requester.getName() + ".").send(player);
            new Message("§e" + player.getName() + " denied your teleportation request.").send(requester);

            return true;
        }

        if(strings[0].equalsIgnoreCase("list")) {
            Set<Player> incomingRequests = new HashSet<>();

            for(Map.Entry<Player, Player> request : requests.entrySet())
                if(request.getValue() == player)
                    incomingRequests.add(request.getKey());

            new Message("§6Pending incoming teleportation requests: " + incomingRequests.size()).send(player);

            for(Player incoming : incomingRequests)
                new Message("└§a§lINCOMING §6" + incoming.getName()).addButton("Accept", ChatColor.GREEN, "Accept this request", "tpa accept " + incoming.getName()).send(player);

            Player outgoingRequest = requests.get(player);
            if(outgoingRequest != null)
                new Message("└§2§lOUTGOING §6" + outgoingRequest.getName()).addButton("Cancel", ChatColor.GRAY, "Cancel your teleportation request", "tpa cancel").send(player);

            return true;
        }

        if(strings[0].equalsIgnoreCase("toggle-allow-incoming")) {
            PlayerDataManager playerDataManager = new PlayerDataManager(player);
            boolean setTo = !(boolean) playerDataManager.get(PlayerDataManager.Attribute.DISALLOW_INCOMING_TPA_REQUESTS);
            playerDataManager.set(PlayerDataManager.Attribute.DISALLOW_INCOMING_TPA_REQUESTS, setTo);

            new SuccessMessage((setTo ? "Disabled" : "Enabled") + " incoming TPA requests. Other players can " + (setTo ? "no longer" : "now") + " TPA to you.").send(player);

            if(setTo) {
                for(Map.Entry<Player, Player> request : requests.entrySet())
                    if(request.getValue() == player) {
                        new NoteMessage("You already have incoming TPA requests pending. These are kept. New requests will be canceled.").send(player);
                        break;
                    }
            }

            return true;
        }

        if(requests.containsKey(player)) {
            new ErrorMessage(ErrorMessage.ErrorType.COMMON, "You have already made a teleportation request. Cancel it before you can make another one.").send(player);
            return true;
        }

        UUID targetUUID = new UsernameStorageManager().getUUIDFromUsernameOrUUID(strings[0]);

        if(targetUUID == null) {
            new ErrorMessage(ErrorMessage.ErrorType.COMMON, "That player is not registered on this server.").send(player);
            return true;
        }

        Player targetPlayer = NoxetServer.getPlugin().getServer().getPlayer(targetUUID);

        if(targetPlayer == null) {
            new ErrorMessage(ErrorMessage.ErrorType.COMMON, "That player is not present on Noxet.org right now.").send(player);
            return true;
        }

        if(targetPlayer == player) {
            new ErrorMessage(ErrorMessage.ErrorType.COMMON, "You cannot teleport to yourself!").send(player);
            return true;
        }

        RealmManager.Realm targetRealm = RealmManager.getCurrentRealm(targetPlayer);

        if(targetRealm == null) {
            new ErrorMessage(ErrorMessage.ErrorType.COMMON, targetPlayer.getName() + " is not in a realm.").send(player);
            return true;
        }

        if(realm != targetRealm) {
            new ErrorMessage(ErrorMessage.ErrorType.COMMON, targetPlayer.getName() + " is not in " + realm.getDisplayName() + ". They are in " + targetRealm.getDisplayName() + "! You must move to that realm first.").send(player);
            return true;
        }

        if(
                new PlayerDataManager(targetPlayer).doesContain(PlayerDataManager.Attribute.BLOCKED_PLAYERS, player.getUniqueId().toString()) ||
                (boolean) new PlayerDataManager(targetPlayer).get(PlayerDataManager.Attribute.DISALLOW_INCOMING_TPA_REQUESTS)
        ) {
            new ErrorMessage(ErrorMessage.ErrorType.COMMON, "You may not send TPA requests to this player.").send(player);
            return true;
        }

        requests.put(player, targetPlayer);

        new Message("§eSent a teleportation request to " + targetPlayer.getName() + ".").send(player);
        new Message(null).addButton("Cancel", ChatColor.RED, "Cancel your request", "tpa cancel").send(player);

        new Message("§6" + player.getName() + "§e has sent you a teleportation request.").send(targetPlayer);
        new Message(null)
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
                /*.addButton(
                        "Block",
                        ChatColor.DARK_GRAY,
                        "Block this player entirely",
                        "block " + player.getName())*/
                .send(targetPlayer);

        if(
                Friend.areFriends(player.getUniqueId(), targetPlayer.getUniqueId()) &&
                        (boolean) new PlayerDataManager(targetPlayer).get(PlayerDataManager.Attribute.FRIEND_TELEPORTATION)
        ) {
            new Message("§b" + targetPlayer.getName() + " has friendly teleportation enabled. They will automatically accept in §35§b seconds.").send(player);
            new Message("§b" + player.getName() + " is your friend, and you have friendly teleportation enabled. The request will be automatically accepted in §35§b seconds.").send(targetPlayer);

            new BukkitRunnable() {
                @Override
                public void run() {
                    targetPlayer.performCommand("tpa accept " + player.getName());
                }
            }.runTaskLater(NoxetServer.getPlugin(), 20 * 5);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                if(requests.remove(player, targetPlayer)) { // If value was removed; was still active.
                    // Meaning that the request was still valid. But now it has expired.
                    new Message("§cYour teleportation request to " + targetPlayer.getDisplayName() + " has expired.").send(player);
                }
            }
        }.runTaskLater(NoxetServer.getPlugin(), 20 * 60);

        return true;
    }

    public static void abortPlayerRelatedRequests(Player player) {
        if(requests.containsKey(player)) {
            new Message("§cThe teleportation request from " + player.getName() + " has been aborted because they left the realm.").send(requests.get(player));
            requests.remove(player); // Remove request FROM player.
        }

        for(Map.Entry<Player, Player> request : requests.entrySet())
            if(request.getValue() == player) {
                new Message("§cYour teleportation request to " + player.getName() + " has been aborted because they left the realm.").send(request.getKey());
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
            completions.addAll(Arrays.asList("cancel", "accept", "deny", "list"));

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
