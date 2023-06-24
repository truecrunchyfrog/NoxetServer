package org.noxet.noxetserver.messaging;

import net.md_5.bungee.api.ChatColor;

public class NoteMessage extends Message {
    public NoteMessage(String text) {
        super(text);
    }

    @Override
    public ChatColor getDefaultColor() {
        return ChatColor.LIGHT_PURPLE;
    }

    @Override
    public String getPrefix() {
        return super.getPrefix() + "§5Note: ";
    }
}
