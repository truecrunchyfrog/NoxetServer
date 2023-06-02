package org.noxet.noxetserver.messaging;

import net.md_5.bungee.api.ChatColor;

public class NoxetWarningMessage extends NoxetMessage {
    public NoxetWarningMessage(String text) {
        super(text);
    }

    @Override
    public ChatColor getDefaultColor() {
        return ChatColor.YELLOW;
    }

    @Override
    public String getMessagePrefix() {
        return super.getMessagePrefix() + "§c§lWARNING: ";
    }
}
