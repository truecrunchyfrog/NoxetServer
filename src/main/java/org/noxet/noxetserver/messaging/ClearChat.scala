package org.noxet.noxetserver.messaging;

class ClearChat extends Message(ClearChat.getClearChatMessage)

object ClearChat:
    private def getClearChatMessage: String = "\n".repeat(200)