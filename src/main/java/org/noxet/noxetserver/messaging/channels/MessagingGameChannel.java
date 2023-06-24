package org.noxet.noxetserver.messaging.channels;

import org.bukkit.command.CommandSender;
import org.noxet.noxetserver.minigames.MiniGameController;

import java.util.HashSet;
import java.util.Set;

public class MessagingGameChannel extends MessagingChannel {
    private final MiniGameController miniGameController;

    public MessagingGameChannel(MiniGameController miniGameController) {
        this.miniGameController = miniGameController;
    }

    @Override
    public Set<CommandSender> getRecipients() {
        return new HashSet<>(miniGameController.getPlayersAndSpectators());
    }
}
