package org.noxet.noxetserver.minigames.party;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;
import org.noxet.noxetserver.messaging.ErrorMessage;
import org.noxet.noxetserver.messaging.Message;
import org.noxet.noxetserver.messaging.MessagingContext;
import org.noxet.noxetserver.messaging.channels.MessagingPartyChannel;
import org.noxet.noxetserver.minigames.MiniGameManager;
import org.noxet.noxetserver.playerdata.PlayerDataManager;
import org.noxet.noxetserver.util.TextBeautifier;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Party {
    private static final Map<Player, Party> partyMembers = new HashMap<>();

    private Player owner;
    private final Set<Player> members = new HashSet<>(), invitedPlayers = new HashSet<>();
    private final MessagingContext messagingContext;

    public Party(Player owner) {
        this.owner = owner;

        messagingContext = new MessagingContext("§e§l" + TextBeautifier.beautify("Party") + " ", new MessagingPartyChannel(this));

        addMember(owner);

        sendPartyMessage(new Message("§eParty created! Use §d/party invite §5<player>§e to invite a player to the party."));
    }

    public void addMember(Player newMember) {
        if(newMember != owner) {
            new Message("§aYou were added to the party.").send(newMember);
            sendPartyMessage(new Message("§a" + newMember.getName() + " is now a party member."));
        }

        members.add(newMember);
        partyMembers.put(newMember, this);
    }

    public void removeMember(Player member) {
        if(!members.contains(member))
            return;

        if(member == owner) {
            disband();
            return;
        }

        members.remove(member);
        partyMembers.remove(member);
    }

    /**
     * When a member leaves the party by themselves.
     * @param member The player who wants to leave the party
     */
    public void memberLeave(Player member) {
        removeMember(member);

        new Message("§aYou left " + owner.getName() + "'s party.").send(member);
        sendPartyMessage(new Message("§c" + member.getName() + " left the party."));
    }

    public void kickMember(Player member) {
        removeMember(member);

        sendPartyMessage(new Message("§c" + member.getName() + " was kicked from the party."));
        new Message("§cYou were kicked from " + owner.getName() + "'s party.").send(member);
    }

    public void invitePlayer(Player player) {
        if(members.contains(player)) {
            new ErrorMessage(ErrorMessage.ErrorType.COMMON, "This player is already in the party.").send(owner);
            return;
        }

        PlayerDataManager playerDataManager = new PlayerDataManager(player);

        if(
                playerDataManager.doesContain(PlayerDataManager.Attribute.BLOCKED_PLAYERS, owner.getUniqueId().toString()) ||
                (boolean) playerDataManager.get(PlayerDataManager.Attribute.DISALLOW_INCOMING_PARTY_INVITES)
        ) {
            new ErrorMessage(ErrorMessage.ErrorType.COMMON, "You may not invite this player.").send(owner);
            return;
        }

        if(invitedPlayers.add(player)) {
            new Message("§eYou have been invited to " + owner + "'s party.\n")
                    .addButton(
                            "Accept",
                            ChatColor.GREEN,
                            "Join this party",
                            "party accept " + owner.getName()
                    )
                    .addButton(
                            "Deny",
                            ChatColor.RED,
                            "Deny this invitation",
                            "party deny " + owner.getName()
                    ).send(player);
            new Message("§eInvited " + player.getName() + " to the party.").send(owner);
        } else
            new ErrorMessage(ErrorMessage.ErrorType.COMMON, "This player has already been invited to the party.").send(owner);
    }

    public void acceptInvite(Player player) {
        if(members.size() >= 50) {
            new ErrorMessage(ErrorMessage.ErrorType.COMMON, "Sorry, there are too many members in this party!").send(player);
            sendPartyMessage(new Message("§c" + player.getName() + " cannot join the game, because there are too many members."));
            return;
        }

        if(!invitedPlayers.remove(player))
            return;

        addMember(player);
    }

    public void denyInvite(Player player) {
        if(!invitedPlayers.remove(player))
            return;

        new Message("§aYou denied the invite from " + owner.getName() + ".").send(player);
        new Message("§c" + player.getName() + " refused your invite.").send(owner);
    }

    public boolean isPlayerInvited(Player player) {
        return invitedPlayers.contains(player);
    }

    /**
     * Gets the members of this party. The owner is also a member!
     * @return Set of the members
     */
    public Set<Player> getMembers() {
        return members;
    }

    public Player getOwner() {
        return owner;
    }

    public void sendPartyMessage(Message message) {
        messagingContext.broadcast(message);
    }

    /**
     * Disband the party.
     */
    public void disband() {
        sendPartyMessage(new Message("§cThe party has disbanded."));
        partyMembers.keySet().removeAll(members);
    }

    public void transfer(Player newOwner) {
        if(!members.contains(newOwner)) {
            new ErrorMessage(ErrorMessage.ErrorType.COMMON, "You cannot transfer party ownership to players outside the party.").send(owner);
            return;
        }

        if(isOwner(newOwner)) {
            new ErrorMessage(ErrorMessage.ErrorType.COMMON, "You are already the owner!").send(owner);
            return;
        }

        sendPartyMessage(new Message("§eThe party is now owned by " + newOwner.getName() + " (previously " + newOwner.getName() + ")."));
        new Message("§aYou are now the owner of this party.").send(newOwner);

        owner = newOwner;
    }

    public boolean isOwner(Player member) {
        return member == owner;
    }

    public static Party getPartyFromMember(Player member) {
        return partyMembers.get(member);
    }

    public static boolean isPlayerMemberOfParty(Player player) {
        return partyMembers.containsKey(player);
    }

    public static Party getOwnersParty(Player owner) {
        Party playersParty = getPartyFromMember(owner);
        return playersParty.isOwner(owner) ? playersParty : null;
    }

    public Set<Player> getBusyMembers() {
        Set<Player> busyMembers = new HashSet<>();

        for(Player member : members)
            if(member != owner && MiniGameManager.isPlayerBusyInGame(member))
                busyMembers.add(member);

        return busyMembers;
    }

    /**
     * Check whether the party can summon all members to a game (checks if players are busy already, to prevent).
     * @return Whether a game can be joined
     */
    public boolean isPartyReadyForGame() {
        return getBusyMembers().isEmpty();
    }

    public void kickBusyPlayers() {
        for(Player busyMember : getBusyMembers())
            kickMember(busyMember);
    }

    public static void abandonPlayer(Player player) {
        Party party = getPartyFromMember(player);
        if(party != null)
            party.removeMember(player);
    }
}
