package org.noxet.noxetserver.messaging;

import net.md_5.bungee.api.ChatMessageType;

public class ActionBarMessage extends Message {
    /**
     * Constructs a message with a text.
     * @param text The text message to be sent
     */
    public ActionBarMessage(String text) {
        super(text);
        chatMessageType = ChatMessageType.ACTION_BAR;
        setPrefix(null);
    }
}
