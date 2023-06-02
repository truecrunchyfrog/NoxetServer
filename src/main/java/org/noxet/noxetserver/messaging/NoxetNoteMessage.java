package org.noxet.noxetserver.messaging;

import net.md_5.bungee.api.ChatColor;

public class NoxetNoteMessage extends NoxetMessage {
    public NoxetNoteMessage(String text) {
        super(text);
    }

    @Override
    public ChatColor getDefaultColor() {
        return ChatColor.LIGHT_PURPLE;
    }

    @Override
    public String getMessagePrefix() {
        return super.getMessagePrefix() + "§5Note: ";
    }
}
