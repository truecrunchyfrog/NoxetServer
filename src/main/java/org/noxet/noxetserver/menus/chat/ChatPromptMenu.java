package org.noxet.noxetserver.menus.chat;

import com.google.common.collect.Sets;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Consumer;
import org.noxet.noxetserver.NoxetServer;
import org.noxet.noxetserver.messaging.NoxetMessage;

import java.util.HashSet;
import java.util.Set;

public class ChatPromptMenu implements Listener {
    public static class PromptResponse {
        private final Player responder;
        private final String message;

        public PromptResponse(Player responder, String message) {
            this.responder = responder;
            this.message = message;
        }

        public Player getResponder() {
            return responder;
        }

        public String getMessage() {
            return message;
        }
    }

    private final String message;
    private final Set<Player> promptedPlayers;
    private final Consumer<PromptResponse> callback;

    public ChatPromptMenu(String message, Set<Player> promptedPlayers, Consumer<PromptResponse> callback) {
        this.message = message;
        this.promptedPlayers = promptedPlayers;
        this.callback = callback;

        NoxetServer.getPlugin().getServer().getPluginManager().registerEvents(this, NoxetServer.getPlugin());

        for(Player player : promptedPlayers)
            promptPlayer(player);
    }

    public ChatPromptMenu(String message, Player promptedPlayer, Consumer<PromptResponse> callback) {
        this(message, Sets.newHashSet(promptedPlayer), callback);
    }

    public void promptPlayer(Player player) {
        new NoxetMessage("§8- - - - - -").send(player);
        new NoxetMessage("§3Please type §7" + message + "§3 and press enter:").send(player);
    }

    public void dispatchPlayerMessage(Player player, String value) {
        promptedPlayers.remove(player);
        if(promptedPlayers.isEmpty())
            stop();
        callback.accept(new PromptResponse(player, value));
    }

    @EventHandler
    public void onAsyncPlayerChat(AsyncPlayerChatEvent e) {
        if(promptedPlayers.contains(e.getPlayer())) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    dispatchPlayerMessage(e.getPlayer(), e.getMessage());
                }
            }.runTaskLater(NoxetServer.getPlugin(), 0);
            e.setCancelled(true);
        }
    }

    public void stop() {
        HandlerList.unregisterAll(this);
    }
}
