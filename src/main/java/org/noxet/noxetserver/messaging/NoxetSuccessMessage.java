package org.noxet.noxetserver.messaging;

import net.md_5.bungee.api.ChatColor;

public class NoxetSuccessMessage extends NoxetMessage {
    public NoxetSuccessMessage(String text) {
        super(text);
    }

    @Override
    public ChatColor getDefaultColor() {
        return ChatColor.GREEN;
    }

    @Override
    public String getMessagePrefix() {
        return super.getMessagePrefix() + "§2Success: ";
    }
}
