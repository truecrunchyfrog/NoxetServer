package org.noxet.noxetserver.messaging;

import net.md_5.bungee.api.ChatColor;

public class SuccessMessage extends Message {
    public SuccessMessage(String text) {
        super(text);
    }

    @Override
    public ChatColor getDefaultColor() {
        return ChatColor.GREEN;
    }

    @Override
    public String getPrefix() {
        return super.getPrefix() + "§2Success: ";
    }
}
