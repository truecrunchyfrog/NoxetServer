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
import org.noxet.noxetserver.RealmManager;
import org.noxet.noxetserver.UsernameStorageManager;
import org.noxet.noxetserver.messaging.NoxetErrorMessage;
import org.noxet.noxetserver.messaging.NoxetMessage;
import org.noxet.noxetserver.messaging.NoxetNoteMessage;
import org.noxet.noxetserver.messaging.NoxetSuccessMessage;
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
            new NoxetErrorMessage(NoxetErrorMessage.ErrorType.COMMON, "Only players can make teleportation requests.").send(commandSender);
            return true;
        }

        Player player = (Player) commandSender;

        RealmManager.Realm realm = RealmManager.getCurrentRealm(player);

        if(realm == null) {
            new NoxetErrorMessage(NoxetErrorMessage.ErrorType.COMMON, "You must be in a realm to do this.").send(player);
            return true;
        }

        if(!realm.doesAllowTeleportationMethods()) {
            new NoxetErrorMessage(NoxetErrorMessage.ErrorType.COMMON, "This realm does not allow TPA.").send(player);
            return true;
        }

        if(strings.length == 0) {
            new NoxetErrorMessage(NoxetErrorMessage.ErrorType.ARGUMENT, "Missing argument for player to send the request to (or cancel/accept/deny).").send(player);
            return true;
        }

        if(strings[0].equalsIgnoreCase("cancel")) {
            if(!requests.containsKey(player)) {
                new NoxetErrorMessage(NoxetErrorMessage.ErrorType.COMMON, "You have not sent any request.").send(player);
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
                new NoxetErrorMessage(NoxetErrorMessage.ErrorType.COMMON, "You have not received a teleportation request.").send(player);
                return true;
            }

            Player specificPlayer = null;

            if(strings.length > 1) {
                if(Objects.equals(strings[1], "*")) {
                    new NoxetMessage("§eAccepting all incoming requests...").send(player);

                    for(Map.Entry<Player, Player> request : requests.entrySet())
                        if(request.getValue() == player)
                            player.performCommand("tpa accept " + request.getKey().getName());

                    return true;
                }


                specificPlayer = NoxetServer.getPlugin().getServer().getPlayer(strings[1]);

                if(specificPlayer == null) {
                    new NoxetErrorMessage(NoxetErrorMessage.ErrorType.COMMON, "That player is not present on Noxet.org right now.").send(player);
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
                    new NoxetErrorMessage(NoxetErrorMessage.ErrorType.COMMON, specificPlayer.getDisplayName() + " has not sent a teleportation request to you.").send(player);
                    return true;
                }

                requester = specificPlayer;
            }

            assert requester != null;

            new NoxetMessage("§eAccepting request...").send(player);
            new NoxetMessage("§e" + player.getDisplayName() + " accepted your teleportation request.").send(requester);

            if(!TeleportUtil.isLocationTeleportSafe(player.getLocation())) {
                new NoxetErrorMessage(NoxetErrorMessage.ErrorType.COMMON, "We blocked this teleportation because it appears to be unsafe. " + player.getDisplayName() + ", please move to a solid block and accept from there.").send(Arrays.asList(player, requester));
                return true;
            }

            requests.remove(requester);

            Events.setTemporaryInvulnerability(player);
            requester.teleport(player);
            requester.getWorld().spawnParticle(Particle.DRAGON_BREATH, requester.getLocation(), 56);

            new NoxetMessage("§e" + requester.getDisplayName() + " has teleported to you.").send(player);
            new NoxetMessage("§eYou have been teleported.").send(requester);

            return true;
        }

        if(strings[0].equalsIgnoreCase("deny")) {
            if(!requests.containsValue(player)) {
                new NoxetErrorMessage(NoxetErrorMessage.ErrorType.COMMON, "You have not received a teleportation request.").send(player);
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
                    new NoxetErrorMessage(NoxetErrorMessage.ErrorType.COMMON, "That player is not present on Noxet.org right now.").send(player);
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
                    new NoxetErrorMessage(NoxetErrorMessage.ErrorType.COMMON, specificPlayer.getDisplayName() + " has not sent a teleportation request to you.").send(player);
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

        if(strings[0].equalsIgnoreCase("list")) {
            Set<Player> incomingRequests = new HashSet<>();

            for(Map.Entry<Player, Player> request : requests.entrySet())
                if(request.getValue() == player)
                    incomingRequests.add(request.getKey());

            new NoxetMessage("§6Pending incoming teleportation requests: " + incomingRequests.size()).send(player);

            for(Player incoming : incomingRequests)
                new NoxetMessage("§a§lINCOMING §6" + incoming.getDisplayName()).addButton("Accept", ChatColor.GREEN, "Accept this request", "tpa accept " + incoming.getName()).send(player);

            Player outgoingRequest = requests.get(player);
            if(outgoingRequest != null)
                new NoxetMessage("§2§lOUTGOING §6" + outgoingRequest.getDisplayName()).addButton("Cancel", ChatColor.GRAY, "Cancel your teleportation request", "tpa cancel").send(player);

            return true;
        }

        if(strings[0].equalsIgnoreCase("block")) {
            if(strings.length < 2) {
                new NoxetErrorMessage(NoxetErrorMessage.ErrorType.ARGUMENT, "Missing argument: player to TPA block.").send(player);
                return true;
            }

            String uuidOrUsername = strings[1];

            UUID uuidToBlock = new UsernameStorageManager().getUUIDFromUsernameOrUUID(uuidOrUsername);

            if(uuidToBlock == null) {
                new NoxetErrorMessage(NoxetErrorMessage.ErrorType.COMMON, "A player by the name '" + uuidOrUsername + "' has never played on Noxet.org.").send(player);
                return true;
            }

            if(player.getUniqueId().equals(uuidToBlock)) {
                new NoxetErrorMessage(NoxetErrorMessage.ErrorType.COMMON, "You cannot block yourself.").send(player);
                return true;
            }

            List<String> blockedPlayers = getBlockedPlayers(player);

            if(blockedPlayers.contains(uuidToBlock.toString())) {
                new NoxetErrorMessage(NoxetErrorMessage.ErrorType.COMMON, "You have already blocked this player.").send(player);
                return true;
            }

            Player playerToBlock = NoxetServer.getPlugin().getServer().getPlayer(uuidToBlock);
            if(playerToBlock != null && requests.remove(playerToBlock, player))
                new NoxetNoteMessage("Silently denied teleport request.").send(player);

            blockedPlayers.add(uuidToBlock.toString());
            new PlayerDataManager(player).set(PlayerDataManager.Attribute.TPA_BLOCKED_PLAYERS, blockedPlayers).save();

            new NoxetSuccessMessage(UsernameStorageManager.getCasedUsernameFromUUID(uuidToBlock) + " can no longer TPA to you.")
                    .addButton(
                            "Undo",
                            ChatColor.YELLOW,
                            "Unblock",
                            "tpa unblock " + uuidToBlock.toString())
                    .send(player);

            return true;
        }

        if(strings[0].equalsIgnoreCase("unblock")) {
            if(strings.length < 2) {
                new NoxetErrorMessage(NoxetErrorMessage.ErrorType.ARGUMENT, "You must provide the name of a player to TPA unblock.").send(player);
                return true;
            }

            String uuidOrUsername = strings[1];

            UUID uuidToUnblock = new UsernameStorageManager().getUUIDFromUsernameOrUUID(uuidOrUsername);

            List<String> blockedPlayers = getBlockedPlayers(player);

            if(!blockedPlayers.remove(uuidToUnblock.toString())) {
                new NoxetErrorMessage(NoxetErrorMessage.ErrorType.COMMON, "You have not blocked this player.").send(player);
                return true;
            }

            new PlayerDataManager(player).set(PlayerDataManager.Attribute.TPA_BLOCKED_PLAYERS, blockedPlayers).save();
            new NoxetSuccessMessage(UsernameStorageManager.getCasedUsernameFromUUID(uuidToUnblock) + " can now TPA to you again.").send(player);

            return true;
        }

        if(strings[0].equalsIgnoreCase("block-list")) {
            List<String> blockedPlayers = getBlockedPlayers(player);

            new NoxetMessage("§eTPA blocked players: " + blockedPlayers.size()).send(player);

            for(String blockedPlayerUUID : blockedPlayers)
                new NoxetMessage("§c§lBLOCKED §6" + UsernameStorageManager.getCasedUsernameFromUUID(UUID.fromString(blockedPlayerUUID)))
                        .addButton("Pardon", ChatColor.GREEN, "Unblock this player", "tpa unblock " + blockedPlayerUUID)
                        .send(player);

            return true;
        }

        if(requests.containsKey(player)) {
            new NoxetErrorMessage(NoxetErrorMessage.ErrorType.COMMON, "You have already made a teleportation request. Cancel it before you can make another one.").send(player);
            return true;
        }

        Player targetPlayer = NoxetServer.getPlugin().getServer().getPlayer(strings[0]);

        if(targetPlayer == null) {
            new NoxetErrorMessage(NoxetErrorMessage.ErrorType.COMMON, "That player is not present on Noxet.org right now.").send(player);
            return true;
        }

        if(targetPlayer == player) {
            new NoxetErrorMessage(NoxetErrorMessage.ErrorType.COMMON, "You cannot teleport to yourself!").send(player);
            return true;
        }

        RealmManager.Realm targetRealm = RealmManager.getCurrentRealm(targetPlayer);

        if(targetRealm == null) {
            new NoxetErrorMessage(NoxetErrorMessage.ErrorType.COMMON, targetPlayer.getDisplayName() + " is not in a realm.").send(player);
            return true;
        }

        if(realm != targetRealm) {
            new NoxetErrorMessage(NoxetErrorMessage.ErrorType.COMMON, targetPlayer.getDisplayName() + " is not in " + realm.getDisplayName() + ". They are in " + targetRealm.getDisplayName() + "! You must move to that realm first.").send(player);
            return true;
        }

        if(getBlockedPlayers(targetPlayer).contains(player.getName().toLowerCase())) {
            new NoxetErrorMessage(NoxetErrorMessage.ErrorType.COMMON, "You cannot send TPA requests to this player.").send(player);
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
                .addButton(
                        "Block",
                        ChatColor.DARK_GRAY,
                        "Don't let this player teleport ask you",
                        "tpa block " + player.getName())
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

    public static List<String> getBlockedPlayers(Player player) {
        return (List<String>) new PlayerDataManager(player).get(PlayerDataManager.Attribute.TPA_BLOCKED_PLAYERS);
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
            completions.addAll(Arrays.asList("cancel", "accept", "deny", "list", "block", "unblock", "block-list"));

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
                case "unblock":
                    List<String> blockedPlayers = getBlockedPlayers(player);

                    for(String blockedPlayerUUID : blockedPlayers) {
                        String playerName = NoxetServer.getPlugin().getServer().getOfflinePlayer(UUID.fromString(blockedPlayerUUID)).getName();
                        completions.add(playerName != null ? playerName : blockedPlayerUUID);
                    }

                    break;
            }
        }

        return completions;
    }
}
