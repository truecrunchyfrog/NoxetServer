package org.noxet.noxetserver.messaging.channels;

import org.bukkit.World;
import org.bukkit.command.CommandSender;

import java.util.HashSet;
import java.util.Set;

public class MessagingWorldChannel extends MessagingChannel {
    private final World world;

    public MessagingWorldChannel(World world) {
        this.world = world;
    }

    @Override
    public Set<CommandSender> getRecipients() {
        return new HashSet<>(world.getPlayers());
    }
}
