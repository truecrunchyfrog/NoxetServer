package org.noxet.noxetserver.messaging;

object ClearChat extends Message(ClearChat.getClearChatMessage):
    private def getClearChatMessage: String = "\n".repeat(200)