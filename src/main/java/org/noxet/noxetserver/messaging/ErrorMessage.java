package org.noxet.noxetserver.messaging;

import net.md_5.bungee.api.ChatColor;

public class ErrorMessage extends Message {
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

    public ErrorMessage(ErrorType errorType, String text) {
        super(text);
        this.errorType = errorType;
    }

    @Override
    public ChatColor getDefaultColor() {
        return ChatColor.RED;
    }

    @Override
    public String getPrefix() {
        return super.getPrefix() + "§4Error" + (errorType.getTypeString() != null ? " (§7" + errorType.getTypeString() + "§4)" : "") + ": ";
    }
}
