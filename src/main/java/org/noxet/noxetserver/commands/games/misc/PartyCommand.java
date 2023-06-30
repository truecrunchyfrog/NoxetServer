package org.noxet.noxetserver.commands.games.misc;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.noxet.noxetserver.NoxetServer;
import org.noxet.noxetserver.menus.inventory.PartyMenu;
import org.noxet.noxetserver.messaging.ErrorMessage;
import org.noxet.noxetserver.messaging.Message;
import org.noxet.noxetserver.messaging.NoteMessage;
import org.noxet.noxetserver.messaging.SuccessMessage;
import org.noxet.noxetserver.minigames.party.Party;
import org.noxet.noxetserver.playerdata.PlayerDataManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PartyCommand implements TabExecutor {
    @Override
    @SuppressWarnings("NullableProblems")
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(!(commandSender instanceof Player)) {
            new ErrorMessage(ErrorMessage.ErrorType.COMMON, "Only players can manage parties.").send(commandSender);
            return true;
        }

        Player player = (Player) commandSender;

        if(strings.length == 0) {
            new PartyMenu(player).openInventory(player);
            return true;
        }

        if(strings[0].equalsIgnoreCase("create")) {
            if(Party.isPlayerMemberOfParty(player)) {
                new ErrorMessage(ErrorMessage.ErrorType.COMMON, "You are already in a party. Please leave the party first.").send(player);
                return true;
            }

            new Party(player);

            return true;
        } else if(strings[0].equalsIgnoreCase("invite")) {
            if(strings.length < 2) {
                new ErrorMessage(ErrorMessage.ErrorType.ARGUMENT, "Missing argument: player to invite.").send(player);
                return true;
            }

            Player playerToInvite = NoxetServer.getPlugin().getServer().getPlayer(strings[1]);

            if(playerToInvite == null) {
                new ErrorMessage(ErrorMessage.ErrorType.COMMON, "Invalid player. You can only invite someone who is actually online.").send(player);
                return true;
            }

            Party party = Party.getPartyFromMember(player);

            if(party == null) {
                new NoteMessage("You are not in a party. To save you time, we will create one for you.").send(player);
                party = new Party(player);
            }

            if(!party.isOwner(player)) {
                new ErrorMessage(ErrorMessage.ErrorType.PERMISSION, "You are not the owner of this party, so you cannot invite players.").send(player);
                return true;
            }

            party.invitePlayer(playerToInvite);

            return true;
        } else if(strings[0].equalsIgnoreCase("accept")) {
            if(strings.length < 2) {
                new ErrorMessage(ErrorMessage.ErrorType.ARGUMENT, "Missing argument: invitation to accept.").send(player);
                return true;
            }

            Player partyOwner = NoxetServer.getPlugin().getServer().getPlayer(strings[1]);

            if(partyOwner == null) {
                new ErrorMessage(ErrorMessage.ErrorType.COMMON, "Invalid player.").send(player);
                return true;
            }

            Party party = Party.getOwnersParty(partyOwner);

            if(party == null) {
                new NoteMessage("That player is not hosting a party. The party may have disbanded.").send(player);
                return true;
            }

            if(!party.isPlayerInvited(player)) {
                new ErrorMessage(ErrorMessage.ErrorType.COMMON, "You are not invited to this party.").send(player);
                return true;
            }

            party.acceptInvite(player);

            return true;
        } else if(strings[0].equalsIgnoreCase("deny")) {
            if(strings.length < 2) {
                new ErrorMessage(ErrorMessage.ErrorType.ARGUMENT, "Missing argument: invitation to deny.").send(player);
                return true;
            }

            Player partyOwner = NoxetServer.getPlugin().getServer().getPlayer(strings[1]);

            if(partyOwner == null) {
                new ErrorMessage(ErrorMessage.ErrorType.COMMON, "Invalid player.").send(player);
                return true;
            }

            Party party = Party.getOwnersParty(partyOwner);

            if(party == null) {
                new NoteMessage("That player is not hosting a party. The party may have disbanded.").send(player);
                return true;
            }

            if(!party.isPlayerInvited(player)) {
                new ErrorMessage(ErrorMessage.ErrorType.COMMON, "You are not invited to this party.").send(player);
                return true;
            }

            party.denyInvite(player);

            return true;
        } else if(strings[0].equalsIgnoreCase("kick")) {
            Party party = Party.getPartyFromMember(player);

            if(party == null) {
                new ErrorMessage(ErrorMessage.ErrorType.COMMON, "You are not in a party.").send(player);
                return true;
            }

            if(!party.isOwner(player)) {
                new ErrorMessage(ErrorMessage.ErrorType.PERMISSION, "You are not the owner of this party, so you cannot kick players.").send(player);
                return true;
            }

            if(strings.length < 2) {
                new ErrorMessage(ErrorMessage.ErrorType.ARGUMENT, "Missing argument: player to kick.").send(player);
                return true;
            }

            Player playerToKick = NoxetServer.getPlugin().getServer().getPlayer(strings[1]);

            if(playerToKick == null) {
                new ErrorMessage(ErrorMessage.ErrorType.COMMON, "Invalid player.").send(player);
                return true;
            }

            if(!party.getMembers().contains(playerToKick)) {
                new ErrorMessage(ErrorMessage.ErrorType.COMMON, "This player is not a member of your party.").send(player);
                return true;
            }

            if(party.isOwner(playerToKick)) {
                new ErrorMessage(ErrorMessage.ErrorType.COMMON, "Owners must disband the party to leave it.").send(player);
                return true;
            }

            party.kickMember(playerToKick);

            return true;
        } else if(strings[0].equalsIgnoreCase("transfer")) {
            Party party = Party.getPartyFromMember(player);

            if(party == null) {
                new ErrorMessage(ErrorMessage.ErrorType.COMMON, "You are not in a party.").send(player);
                return true;
            }

            if(!party.isOwner(player)) {
                new ErrorMessage(ErrorMessage.ErrorType.PERMISSION, "You are not the owner of this party, so you cannot transfer it.").send(player);
                return true;
            }

            if(strings.length < 2) {
                new ErrorMessage(ErrorMessage.ErrorType.ARGUMENT, "Missing argument: player to transfer the party to.").send(player);
                return true;
            }

            Player newOwner = NoxetServer.getPlugin().getServer().getPlayer(strings[1]);

            if(newOwner == null) {
                new ErrorMessage(ErrorMessage.ErrorType.COMMON, "Invalid player.").send(player);
                return true;
            }

            if(!party.getMembers().contains(newOwner)) {
                new ErrorMessage(ErrorMessage.ErrorType.COMMON, "This player is not a member of your party.").send(player);
                return true;
            }

            party.transfer(newOwner);

            return true;
        } else if(strings[0].equalsIgnoreCase("leave")) {
            Party party = Party.getPartyFromMember(player);

            if(party == null) {
                new ErrorMessage(ErrorMessage.ErrorType.COMMON, "You are not in a party.").send(player);
                return true;
            }

            if(party.isOwner(player)) {
                new ErrorMessage(ErrorMessage.ErrorType.COMMON, "You are the owner of this party. To leave it, you must disband it or transfer it.").send(player);
                return true;
            }

            party.memberLeave(player);

            return true;
        } else if(strings[0].equalsIgnoreCase("list")) {
            Party party = Party.getPartyFromMember(player);

            if(party == null) {
                new ErrorMessage(ErrorMessage.ErrorType.COMMON, "You are not in a party.").send(player);
                return true;
            }

            new Message("§eMembers (with owner): " + party.getMembers().size()).send(commandSender);

            for(Player member : party.getMembers()) {
                Message message = new Message(
                        "└§6§lMEMBER §e" + member.getName()
                );

                if(party.isOwner(player) && !party.isOwner(member))
                    message.addButton(
                        "Kick",
                        ChatColor.RED,
                        "Kick this player from the party",
                        "party kick " + member.getName()
                    ).addButton(
                            "Transfer",
                            ChatColor.BLUE,
                            "Transfer the party ownership to this player",
                            "party transfer " + member.getName()
                    );

                message.send(player);
            }

            return true;
        } else if(strings[0].equalsIgnoreCase("disband")) {
            Party party = Party.getPartyFromMember(player);

            if(party == null) {
                new ErrorMessage(ErrorMessage.ErrorType.COMMON, "You are not in a party.").send(player);
                return true;
            }

            if(!party.isOwner(player)) {
                new ErrorMessage(ErrorMessage.ErrorType.PERMISSION, "You are not the owner of this party, so you cannot disband it.").send(player);
                return true;
            }

            party.disband();

            return true;
        } else if(strings[0].equalsIgnoreCase("kick-busy")) {
            Party party = Party.getPartyFromMember(player);

            if(party == null) {
                new ErrorMessage(ErrorMessage.ErrorType.COMMON, "You are not in a party.").send(player);
                return true;
            }

            if(!party.isOwner(player)) {
                new ErrorMessage(ErrorMessage.ErrorType.PERMISSION, "You are not the owner of this party, so you cannot kick members from it.").send(player);
                return true;
            }

            if(party.isPartyReadyForGame()) {
                new Message("§aThere are no busy players to kick!").send(player);
                return true;
            }

            new SuccessMessage("Kicking in-game players: " + party.getBusyMembers().size()).send(player);

            party.kickBusyPlayers();

            return true;
        } else if(strings[0].equalsIgnoreCase("toggle-invites")) {
            PlayerDataManager playerDataManager = new PlayerDataManager(player);
            playerDataManager.toggleBoolean(PlayerDataManager.Attribute.DISALLOW_INCOMING_PARTY_INVITES);

            boolean newSetting = (boolean) playerDataManager.get(PlayerDataManager.Attribute.DISALLOW_INCOMING_PARTY_INVITES);
            new SuccessMessage("Party invites " + (newSetting ? "enabled" : "disabled") + ". You can " + (newSetting ? "now" : "no longer") + " receive party invites.").send(player);

            return true;
        } else {
            new ErrorMessage(ErrorMessage.ErrorType.ARGUMENT, "Invalid subcommand '" + strings[0] + "'.").send(player);
            return false;
        }
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        if(!(commandSender instanceof Player))
            return null;

        List<String> completions = new ArrayList<>();

        Player player = (Player) commandSender;

        if(strings.length == 1) {
            completions.addAll(Arrays.asList("create", "invite", "kick", "transfer", "leave", "list", "disband", "kick-busy", "toggle-invites"));
        } else if(strings.length == 2) {
            switch(strings[0].toLowerCase()) {
                case "invite":
                case "accept":
                case "deny":
                    Player recommendedPlayer = NoxetServer.getPlugin().getServer().getPlayer(strings[1]);
                    if(recommendedPlayer != null)
                        completions.add(recommendedPlayer.getName());
                    break;
                case "kick":
                case "transfer":
                    Party party = Party.getOwnersParty(player);
                    if(party != null)
                        party.getMembers().forEach(member -> {
                            if(member != party.getOwner())
                                completions.add(member.getName());
                        });
                    break;
            }
        }

        return completions;
    }
}
