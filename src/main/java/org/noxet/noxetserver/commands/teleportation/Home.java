package org.noxet.noxetserver.commands.teleportation;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.noxet.noxetserver.Events;
import org.noxet.noxetserver.RealmManager;
import org.noxet.noxetserver.menus.chat.ChatPromptMenu;
import org.noxet.noxetserver.menus.inventory.HomeNavigationMenu;
import org.noxet.noxetserver.messaging.*;
import org.noxet.noxetserver.playerdata.PlayerDataManager;
import org.noxet.noxetserver.util.TeleportUtil;

import java.util.*;

public class Home implements TabExecutor {
    public static final String defaultHomeName = "main";

    @Override
    @SuppressWarnings("ALL")
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(!(commandSender instanceof Player)) {
            new NoxetErrorMessage(NoxetErrorMessage.ErrorType.COMMON, "Only players can use homes.").send(commandSender);
            return true;
        }

        Player player = (Player) commandSender;

        RealmManager.Realm realm = RealmManager.getCurrentRealm(player);
        if(realm == null) {
            new NoxetErrorMessage(NoxetErrorMessage.ErrorType.COMMON, "You must be in a realm to do this.").send(player);
            return true;
        }

        if(!realm.doesAllowTeleportationMethods()) {
            new NoxetErrorMessage(NoxetErrorMessage.ErrorType.COMMON, "This realm does not allow you to save homes. You can, however, sleep in beds to save your respawn point. You have to manually transport yourself.").send(player);
            return true;
        }

        Map<String, Map<String, Location>> homes = getHomes(player);

        if(strings.length == 0) {
            new NoxetErrorMessage(NoxetErrorMessage.ErrorType.ARGUMENT, "You have to choose what to do.").send(player);
            return false;
        }

        Map<String, Location> realmHomes = homes.getOrDefault(realm.name(), new HashMap<>());

        String homeName = strings.length >= 2 ? strings[1].toLowerCase() : defaultHomeName;

        if(!isHomeNameOk(homeName)) {
            new NoxetErrorMessage(NoxetErrorMessage.ErrorType.ARGUMENT, "Home name must be 1-20 characters.").send(player);
            return true;
        }

        switch(strings[0].toLowerCase()) {
            case "tp":
                Location homeLocation = realmHomes.get(homeName);

                if(homeLocation == null) {
                    new NoxetErrorMessage(NoxetErrorMessage.ErrorType.COMMON, "You do not have a home saved by the name '" + homeName + "'.").addButton("List homes", ChatColor.YELLOW, "See your saved homes", "home list").send(player);
                    return true;
                }

                if(!TeleportUtil.isLocationTeleportSafe(homeLocation)) {
                    if(strings.length < 3) {
                        new NoxetWarningMessage(
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
                    } else if(strings[2].equalsIgnoreCase("force")) {
                        new NoxetMessage("§aForcing teleport to home...").send(player);
                    } else if(strings[2].equalsIgnoreCase("safe")) {
                        new NoxetMessage("§aFinding a safe location nearby...").send(player);
                        homeLocation = TeleportUtil.getSafeTeleportLocation(homeLocation);
                        if(homeLocation == null) {
                            new NoxetErrorMessage(NoxetErrorMessage.ErrorType.COMMON, "We could not find a safe location nearby that home.").send(player);
                            return true;
                        }
                    }
                }

                if(player.teleport(homeLocation)) {
                    Events.setTemporaryInvulnerability(player);
                    new NoxetMessage("§3Welcome home!").send(player);
                } else
                    new NoxetErrorMessage(NoxetErrorMessage.ErrorType.COMMON, "Sorry, you could not be teleported to your home. Please report this.").send(player);
                break;
            case "set":
                if(homeName.equals("?")) {
                    new ChatPromptMenu("home name", player, promptResponse -> player.performCommand("home set " + promptResponse.getMessage()));
                    return true;
                }

                if(realmHomes.containsKey(homeName) && !(strings.length >= 3 && strings[2].equalsIgnoreCase("overwrite"))) {
                    new NoxetErrorMessage(NoxetErrorMessage.ErrorType.COMMON, "You have already saved a home as '" + homeName + "'.").addButton("Overwrite", ChatColor.RED, "Overwrite your existing home by this name", "home set " + homeName + " overwrite").send(player);
                    return true;
                }

                for(Map.Entry<String, Location> homeEntry : realmHomes.entrySet())
                    if(homeEntry.getKey() != homeName && player.getWorld() == homeEntry.getValue().getWorld() && player.getLocation().distance(homeEntry.getValue()) < 50)
                        new NoxetNoteMessage("Your home '" + homeEntry.getKey() + "' is quite near this location.").send(player);

                boolean overwrote = realmHomes.put(homeName, player.getLocation()) != null;

                if(realmHomes.size() > 50) {
                    new NoxetErrorMessage(NoxetErrorMessage.ErrorType.COMMON, "You have reached your home limit. Max 50 homes are allowed per player. Consider deleting other homes before making another one.").send(player);
                    return true;
                }

                homes.put(realm.name(), realmHomes);

                new PlayerDataManager(player).set(PlayerDataManager.Attribute.HOMES, homes).save();

                new NoxetSuccessMessage("Home '" + homeName + "' has been saved.").send(player);
                if(overwrote)
                    new NoxetNoteMessage("Old home location by same name was overwritten.").send(player);
                break;
            case "remove":
                if(!realmHomes.containsKey(homeName)) {
                    new NoxetErrorMessage(NoxetErrorMessage.ErrorType.COMMON, "You do not have a home called '" + homeName + "'.").send(player);
                    return true;
                }

                realmHomes.remove(homeName);
                homes.put(realm.name(), realmHomes);

                new PlayerDataManager(player).set(PlayerDataManager.Attribute.HOMES, homes).save();

                new NoxetSuccessMessage("Home '" + homeName + "' has been removed.").send(player);
                break;
            case "list":
                new NoxetMessage("§eHomes: " + realmHomes.size()).send(player);

                if(realmHomes.isEmpty()) {
                    new NoxetMessage("You don't have any home yet!").addButton("Add home here", ChatColor.GREEN, "Set your default home to here", "home set").send(player);
                    return true;
                }

                for(Map.Entry<String, Location> eachHome : realmHomes.entrySet())
                    new NoxetMessage("§a§lHOME §6" + eachHome.getKey()).addButton("Go", ChatColor.GREEN, "Teleport to '" + eachHome.getKey() + "'", "home tp " + eachHome.getKey()).send(player);
                break;
            case "rename":
                if(!realmHomes.containsKey(homeName)) {
                    new NoxetErrorMessage(NoxetErrorMessage.ErrorType.COMMON, "You do not have a home called '" + homeName + "'.").send(player);
                    return true;
                }

                if(strings.length < 3) {
                    new NoxetErrorMessage(NoxetErrorMessage.ErrorType.ARGUMENT, "Missing argument! Add what the home should be renamed to.").send(player);
                    return true;
                }

                String newName = strings[2];

                if(!isHomeNameOk(newName)) {
                    new NoxetErrorMessage(NoxetErrorMessage.ErrorType.ARGUMENT, "Home name must be 1-20 characters.").send(player);
                    return true;
                }

                if(realmHomes.containsKey(newName)) {
                    new NoxetErrorMessage(NoxetErrorMessage.ErrorType.COMMON, "You already have a home called '" + newName + "'.").send(player);
                    return true;
                }

                realmHomes.put(newName, realmHomes.get(homeName));
                realmHomes.remove(homeName);
                homes.put(realm.name(), realmHomes);

                new PlayerDataManager(player).set(PlayerDataManager.Attribute.HOMES, homes).save();

                new NoxetSuccessMessage("Home '" + homeName + "' has been renamed to '" + newName + "'.").send(player);
                break;
            case "menu":
                new HomeNavigationMenu(player, realmHomes).openInventory(player);
                break;
            default:
                new NoxetErrorMessage(NoxetErrorMessage.ErrorType.ARGUMENT, "Missing what home command to call.").send(player);
                return false;
        }

        return true;
    }

    @Override
    @SuppressWarnings("ALL")
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        if(!(commandSender instanceof Player))
            return null;

        Player player = (Player) commandSender;

        List<String> completions = new ArrayList<>();
        RealmManager.Realm realm = RealmManager.getCurrentRealm(player);

        Map<String, Map<String, Location>> homes = getHomes(player);

        Map<String, Location> realmHomes = realm != null ? homes.getOrDefault(realm.name(), new HashMap<>()) : null;

        if(strings.length == 1) {
            completions.addAll(Arrays.asList("tp", "set", "remove", "list", "rename", "menu"));
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
            }
        }

        return completions;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isHomeNameOk(String name) {
        return name.length() >= 1 && name.length() <= 20;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Map<String, Location>> getHomes(Player player) {
        return (Map<String, Map<String, Location>>) new PlayerDataManager(player).get(PlayerDataManager.Attribute.HOMES);
    }
}
