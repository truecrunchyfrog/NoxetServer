package org.noxet.noxetserver.messaging;

import net.md_5.bungee.api.ChatMessageType;

/**
 * Constructs a message with a text.
 * @param text The text message to be sent
 */
class ActionBarMessage(text: String) extends Message(text):
    chatMessageType = ChatMessageType.ACTION_BAR
    setPrefix(None)