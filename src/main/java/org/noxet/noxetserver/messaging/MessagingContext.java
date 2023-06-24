package org.noxet.noxetserver.messaging;

import org.noxet.noxetserver.messaging.channels.MessagingChannel;

public class MessagingContext {
    private final String prefix;
    private final MessagingChannel channel;

    public MessagingContext(String prefix, MessagingChannel channel) {
        this.prefix = prefix;
        this.channel = channel;
    }

    public void broadcast(Message message) {
        message.setPrefix(prefix);
        message.send(channel);
    }
}
