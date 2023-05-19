package org.noxet.noxetserver.messaging;

public class NoxetErrorMessage extends NoxetMessage {
    public NoxetErrorMessage(String text) {
        super(text);
    }

    @Override
    public String getMessagePrefix() {
        return super.getMessagePrefix() + "§4ERROR: §c";
    }
}
