package org.noxet.noxetserver.messaging.channels;

import org.bukkit.command.CommandSender;
import org.noxet.noxetserver.minigames.party.Party;

import java.util.HashSet;
import java.util.Set;

public class MessagingPartyChannel extends MessagingChannel {
    private final Party party;

    public MessagingPartyChannel(Party party) {
        this.party = party;
    }

    @Override
    public Set<CommandSender> getRecipients() {
        return new HashSet<>(party.getMembers());
    }
}
