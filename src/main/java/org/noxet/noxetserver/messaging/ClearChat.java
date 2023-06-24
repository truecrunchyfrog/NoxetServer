package org.noxet.noxetserver.messaging;

public class ClearChat extends Message {
    public ClearChat() {
        super(getClearChatMessage());
    }

    private static String getClearChatMessage() {
        StringBuilder newLines = new StringBuilder();

        for(int i = 0; i < 200; i++)
            newLines.append('\n');

        return newLines.toString();
    }
}
