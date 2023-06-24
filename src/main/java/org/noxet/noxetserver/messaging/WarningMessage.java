package org.noxet.noxetserver.messaging;

import net.md_5.bungee.api.ChatColor;

public class WarningMessage extends Message {
    public WarningMessage(String text) {
        super(text);
    }

    @Override
    public ChatColor getDefaultColor() {
        return ChatColor.YELLOW;
    }

    @Override
    public String getPrefix() {
        return super.getPrefix() + "§c§lWARNING: ";
    }
}
