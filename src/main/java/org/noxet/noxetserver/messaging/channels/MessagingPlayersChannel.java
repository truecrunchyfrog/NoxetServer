package org.noxet.noxetserver.messaging.channels;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

public class MessagingPlayersChannel extends MessagingChannel {
    private final Set<Player> players;

    public MessagingPlayersChannel(Set<Player> players) {
        this.players = players;
    }

    @Override
    public Set<CommandSender> getRecipients() {
        return new HashSet<>(players);
    }
}
