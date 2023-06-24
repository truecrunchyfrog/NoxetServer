package org.noxet.noxetserver.messaging.channels;

import org.bukkit.command.CommandSender;

import java.util.Set;

public abstract class MessagingChannel {
    public abstract Set<CommandSender> getRecipients();
}
