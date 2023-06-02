package org.noxet.noxetserver.messaging;

import net.md_5.bungee.api.ChatColor;
import org.noxet.noxetserver.messaging.NoxetMessage;

public class NoxetErrorMessage extends NoxetMessage {
    public enum ErrorType {
        COMMON(null),
        PERMISSION("missing permission"),
        ARGUMENT("incorrect argument usage");

        private final String typeString;

        ErrorType(String typeString) {
            this.typeString = typeString;
        }

        public String getTypeString() {
            return typeString;
        }
    }

    private final ErrorType errorType;

    public NoxetErrorMessage(ErrorType errorType, String text) {
        super(text);
        this.errorType = errorType;
    }

    @Override
    public ChatColor getDefaultColor() {
        return ChatColor.RED;
    }

    @Override
    public String getMessagePrefix() {
        return super.getMessagePrefix() + "§4§lError" + (errorType.getTypeString() != null ? " (§f" + errorType.getTypeString() + "§4§l)" : "") + ": ";
    }
}
