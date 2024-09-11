package org.noxet.noxetserver.messaging

import org.noxet.noxetserver.messaging.channels.MessagingChannel

class MessagingContext(prefix: String, channel: MessagingChannel):
    def broadcast(message: Message): Unit =
        message.setPrefix(prefix)
        message.send(channel)