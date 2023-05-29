package org.noxet.noxetserver.messaging;

import net.md_5.bungee.api.ChatColor;

public class NoxetErrorMessage extends NoxetMessage {
    public NoxetErrorMessage(String text) {
        super(text);
    }

    @Override
    public ChatColor getDefaultColor() {
        return ChatColor.RED;
    }

    @Override
    public String getMessagePrefix() {
        return super.getMessagePrefix() + "§4Error: ";
    }
}
