package org.noxet.noxetserver.commands.teleportation;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.noxet.noxetserver.Events;
import org.noxet.noxetserver.realm.RealmManager;
import org.noxet.noxetserver.util.UsernameStorageManager;
import org.noxet.noxetserver.commands.social.Friend;
import org.noxet.noxetserver.menus.chat.ChatPromptMenu;
import org.noxet.noxetserver.menus.inventory.HomeNavigationMenu;
import org.noxet.noxetserver.messaging.*;
import org.noxet.noxetserver.playerdata.PlayerDataManager;
import org.noxet.noxetserver.util.TeleportUtil;

import java.util.*;

public class Home implements TabExecutor {
    public static final String defaultHomeName = "main";

    @Override
    @SuppressWarnings("NullableProblems")
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(!(commandSender instanceof Player)) {
            new ErrorMessage(ErrorMessage.ErrorType.COMMON, "Only players can use homes.").send(commandSender);
            return true;
        }

        Player player = (Player) commandSender;

        RealmManager.Realm realm = RealmManager.getCurrentRealm(player);
        if(realm == null) {
            new ErrorMessage(ErrorMessage.ErrorType.COMMON, "You must be in a realm to do this.").send(player);
            return true;
        }

        if(!realm.doesAllowTeleportationMethods()) {
            new ErrorMessage(ErrorMessage.ErrorType.COMMON, "This realm does not allow you to save homes. You can, however, sleep in beds to save your respawn point. You have to manually transport yourself.").send(player);
            return true;
        }

        if(strings.length == 0) {
            new HomeNavigationMenu(player, realm).openInventory(player);
            return true;
        }

        if(strings[0].equalsIgnoreCase("friend-tp")) {
            if(strings.length == 1) {
                new ErrorMessage(ErrorMessage.ErrorType.ARGUMENT, "Missing argument: friend's home to teleport to.").send(player);
                return false;
            }

            String friendTpId = strings[1];
            int slashIndex = friendTpId.indexOf('/');

            if(slashIndex == -1 || slashIndex == friendTpId.length() - 1) {
                new ErrorMessage(ErrorMessage.ErrorType.ARGUMENT, "Incorrect syntax. The syntax is: 'friend-name/home-name'.").send(player);
                return true;
            }

            String friendName = friendTpId.substring(0, slashIndex);
            String homeName = '*' + friendTpId.substring(slashIndex + 1);

            UUID friendUUID = new UsernameStorageManager().getUUIDFromUsernameOrUUID(friendName);

            if(friendUUID == null) {
                new ErrorMessage(ErrorMessage.ErrorType.COMMON, "Player '" + friendName + "' has never been on this server.").send(player);
                return true;
            }

            String realFriendName = UsernameStorageManager.getCasedUsernameFromUUID(friendUUID);

            if(!Friend.areFriends(player.getUniqueId(), friendUUID)) {
                new ErrorMessage(ErrorMessage.ErrorType.COMMON, "You are not friends with " + realFriendName + ", and cannot teleport to their friend homes.").send(player);
                return true;
            }

            Map<String, Location> friendHomes = getRealmHomes(friendUUID, realm);

            if(!friendHomes.containsKey(homeName)) {
                new ErrorMessage(ErrorMessage.ErrorType.COMMON, realFriendName + " does not friend share a home by that name.").send(player);
                return true;
            }

            Location homeLocation = friendHomes.get(homeName);

            if(!TeleportUtil.isLocationTeleportSafe(homeLocation)) {
                boolean arg = strings.length > 2,
                        safe = arg && strings[2].equalsIgnoreCase("safe"),
                        force = arg && strings[2].equalsIgnoreCase("force");

                if(safe) {
                    new Message("§aFinding a safe location nearby friend's home...").send(player);
                    homeLocation = TeleportUtil.getSafeTeleportLocation(homeLocation);
                    if(homeLocation == null) {
                        new ErrorMessage(ErrorMessage.ErrorType.COMMON, "We could not find a safe location nearby that home.").send(player);
                        return true;
                    }
                } else if(force) {
                    new Message("§aForcing teleport to friend's home...").send(player);
                } else {
                    new WarningMessage(
                            "This friend home is in a suspicious location."
                    ).addButton(
                            "Teleport safely nearby",
                            ChatColor.GREEN,
                            "Let us find a safe location for you to teleport nearby this home",
                            "home friend-tp " + friendTpId + " safe"
                    ).addButton(
                            "Teleport anyway",
                            ChatColor.RED,
                            "Do this at your own risk",
                            "home friend-tp " + friendTpId + " force"
                    ).send(player);

                    return true;
                }
            }

            if(player.teleport(homeLocation)) {
                Events.setTemporaryInvulnerability(player);
                new Message("§3You teleported to " + friendTpId + ".").send(player);
            }

            return true;
        }

        Map<String, Map<String, Location>> homes = getHomes(player);
        Map<String, Location> realmHomes = getRealmHomes(player, realm);

        String homeName = strings.length >= 2 ? strings[1].toLowerCase() : defaultHomeName;

        if(!isHomeNameOk(homeName)) {
            getHomeNameNotOkMessage().send(player);
            return true;
        }

        switch(strings[0].toLowerCase()) {
            case "tp":
                Location homeLocation = realmHomes.get(homeName);

                if(homeLocation == null) {
                    new ErrorMessage(ErrorMessage.ErrorType.COMMON, "You do not have a home saved by the name '" + homeName + "'.").addButton("List homes", ChatColor.YELLOW, "See your saved homes", "home list").send(player);
                    return true;
                }

                if(!TeleportUtil.isLocationTeleportSafe(homeLocation)) {
                    boolean arg = strings.length > 2,
                            safe = arg && strings[2].equalsIgnoreCase("safe"),
                            force = arg && strings[2].equalsIgnoreCase("force");

                    if(safe) {
                        new Message("§aFinding a safe location nearby...").send(player);
                        homeLocation = TeleportUtil.getSafeTeleportLocation(homeLocation);
                        if(homeLocation == null) {
                            new ErrorMessage(ErrorMessage.ErrorType.COMMON, "We could not find a safe location nearby that home.").send(player);
                            return true;
                        }
                    } else if(force) {
                        new Message("§aForcing teleport to home...").send(player);
                    } else {
                        new WarningMessage(
                                "This home may not be safe to teleport to."
                        ).addButton(
                                "Teleport safely nearby",
                                ChatColor.GREEN,
                                "Let us find a safe location for you to teleport nearby your home",
                                "home tp " + homeName + " safe"
                        ).addButton(
                                "Teleport anyway",
                                ChatColor.RED,
                                "Do this at your own risk",
                                "home tp " + homeName + " force"
                        ).send(player);

                        return true;
                    }
                }

                if(player.teleport(homeLocation)) {
                    Events.setTemporaryInvulnerability(player);
                    new Message("§3Welcome home!").send(player);
                } else
                    new ErrorMessage(ErrorMessage.ErrorType.COMMON, "Sorry, you could not be teleported to your home. Please report this.").send(player);
                break;
            case "set":
                if(homeName.equals("?")) {
                    new ChatPromptMenu("home name", player, promptResponse -> player.performCommand("home set " + promptResponse.getMessage()));
                    return true;
                }

                if(realmHomes.containsKey(homeName) && !(strings.length >= 3 && strings[2].equalsIgnoreCase("overwrite"))) {
                    new ErrorMessage(ErrorMessage.ErrorType.COMMON, "You have already saved a home as '" + homeName + "'.").addButton("Overwrite", ChatColor.RED, "Overwrite your existing home by this name", "home set " + homeName + " overwrite").send(player);
                    return true;
                }

                for(Map.Entry<String, Location> homeEntry : realmHomes.entrySet())
                    if(!Objects.equals(homeEntry.getKey(), homeName) && player.getWorld() == homeEntry.getValue().getWorld() && player.getLocation().distance(homeEntry.getValue()) < 50)
                        new NoteMessage("Your home '" + homeEntry.getKey() + "' is quite near this location.").send(player);

                boolean overwrote = realmHomes.put(homeName, player.getLocation()) != null;

                if(realmHomes.size() > 50) {
                    new ErrorMessage(ErrorMessage.ErrorType.COMMON, "You have reached your home limit. Max 50 homes are allowed per player. Consider deleting other homes before making another one.").send(player);
                    return true;
                }

                homes.put(realm.name(), realmHomes);

                new PlayerDataManager(player).set(PlayerDataManager.Attribute.HOMES, homes).save();

                new SuccessMessage("Home '" + homeName + "' has been saved.").send(player);

                if(overwrote)
                    new NoteMessage("Old home location by same name was overwritten.").send(player);

                if(isHomeFriendShared(homeName))
                    new WarningMessage("The home you just created is friend shared! Any friend of yours can use that home.").send(player);

                break;
            case "remove":
                if(!realmHomes.containsKey(homeName)) {
                    new ErrorMessage(ErrorMessage.ErrorType.COMMON, "You do not have a home called '" + homeName + "'.").send(player);
                    return true;
                }

                realmHomes.remove(homeName);
                homes.put(realm.name(), realmHomes);

                new PlayerDataManager(player).set(PlayerDataManager.Attribute.HOMES, homes).save();

                new SuccessMessage("Home '" + homeName + "' has been removed.").send(player);

                if(isHomeFriendShared(homeName))
                    new NoteMessage("That home was friend shared. Your friends can no longer use it either.").send(player);

                break;
            case "list":
                new Message("§eHomes: " + realmHomes.size()).send(player);

                if(realmHomes.isEmpty()) {
                    new Message("You don't have any home yet!").addButton("Add home here", ChatColor.GREEN, "Set your default home to here", "home set").send(player);
                    return true;
                }

                for(Map.Entry<String, Location> eachHome : realmHomes.entrySet())
                    new Message("└§a§lHOME §6" + eachHome.getKey()).addButton("Go", ChatColor.GREEN, "Teleport to '" + eachHome.getKey() + "'", "home tp " + eachHome.getKey()).send(player);

                break;
            case "rename":
                if(!realmHomes.containsKey(homeName)) {
                    new ErrorMessage(ErrorMessage.ErrorType.COMMON, "You do not have a home called '" + homeName + "'.").send(player);
                    return true;
                }

                if(strings.length < 3) {
                    new ErrorMessage(ErrorMessage.ErrorType.ARGUMENT, "Missing argument! Add what the home should be renamed to.").send(player);
                    return true;
                }

                String newName = strings[2];

                if(!isHomeNameOk(newName)) {
                    getHomeNameNotOkMessage().send(player);
                    return true;
                }

                if(realmHomes.containsKey(newName)) {
                    new ErrorMessage(ErrorMessage.ErrorType.COMMON, "You already have a home called '" + newName + "'.").send(player);
                    return true;
                }

                realmHomes.put(newName, realmHomes.get(homeName));
                realmHomes.remove(homeName);
                homes.put(realm.name(), realmHomes);

                new PlayerDataManager(player).set(PlayerDataManager.Attribute.HOMES, homes).save();

                new SuccessMessage("Home '" + homeName + "' has been renamed to '" + newName + "'.").send(player);

                if(isHomeFriendShared(homeName) && !isHomeFriendShared(newName))
                    new WarningMessage("You disabled friend sharing for that home. Your friends can no longer use that home of yours.").send(player);

                if(!isHomeFriendShared(homeName) && isHomeFriendShared(newName))
                    new WarningMessage("You enabled friend sharing for that home. Your friends can now use that home to teleport to.").send(player);

                break;
            default:
                new ErrorMessage(ErrorMessage.ErrorType.ARGUMENT, "Invalid home method: '" + strings[0] + "'.").send(player);
                return false;
        }

        return true;
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        if(!(commandSender instanceof Player))
            return null;

        Player player = (Player) commandSender;

        List<String> completions = new ArrayList<>();
        RealmManager.Realm realm = RealmManager.getCurrentRealm(player);

        Map<String, Location> realmHomes = realm != null ? getRealmHomes(player, realm) : null;

        if(strings.length == 1) {
            completions.addAll(Arrays.asList("tp", "set", "remove", "list", "rename", "friend-tp"));
        } else if(strings.length == 2) {
            switch(strings[0].toLowerCase()) {
                case "tp":
                case "remove":
                case "rename":
                    if(realmHomes == null)
                        break;

                    for(Map.Entry<String, Location> eachHome : realmHomes.entrySet())
                        completions.add(eachHome.getKey());

                    break;
                case "friend-tp":
                    completions.addAll(getFriendsHomes(player, realm));
                    break;
            }
        }

        return completions;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isHomeNameOk(String name) {
        if(name.length() < 3 || name.length() > 20)
            return false;

        for(char token : name.toCharArray())
            if(!(
                    (token >= 'a' && token <= 'z') ||
                    (token >= '0' && token <= '9') ||
                    token == '-' ||
                    (token == '*' && name.lastIndexOf(token) == 0) // Only allow first character as "*". Using that character in the beginning will make it a friend home.
                    ))
                return false;

        return true;
    }

    public static ErrorMessage getHomeNameNotOkMessage() {
        return new ErrorMessage(ErrorMessage.ErrorType.ARGUMENT, "Home name can only consist of 3-20 characters: alphanumeric (a-z, 0-9) and dashes (\"-\").");
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Map<String, Location>> getHomes(UUID uuid) {
        return (Map<String, Map<String, Location>>) new PlayerDataManager(uuid).get(PlayerDataManager.Attribute.HOMES);
    }

    public static Map<String, Map<String, Location>> getHomes(Player player) {
        return getHomes(player.getUniqueId());
    }

    public static Map<String, Location> getRealmHomes(UUID uuid, RealmManager.Realm realm) {
        return getHomes(uuid).getOrDefault(realm.name(), new HashMap<>());
    }

    public static Map<String, Location> getRealmHomes(Player player, RealmManager.Realm realm) {
        return getRealmHomes(player.getUniqueId(), realm);
    }

    public static List<String> getFriendsHomes(Player player, RealmManager.Realm realm) {
        List<String> friendsHomes = new ArrayList<>();

        List<String> friendUUIDs = Friend.getFriendList(player.getUniqueId());

        for(String eachFriend : friendUUIDs) {
            UUID friendUUID = new UsernameStorageManager().getUUIDFromUsernameOrUUID(eachFriend);
            String friendName = UsernameStorageManager.getCasedUsernameFromUUID(friendUUID);

            if(friendUUID == null)
                continue;

            for(Map.Entry<String, Location> eachFriendHomes : getRealmHomes(friendUUID, realm).entrySet())
                if(isHomeFriendShared(eachFriendHomes.getKey()))
                    friendsHomes.add(friendName + "/" + eachFriendHomes.getKey().substring(1));
        }

        return friendsHomes;
    }

    public static boolean isHomeFriendShared(String homeName) {
        return homeName.startsWith("*");
    }
}
