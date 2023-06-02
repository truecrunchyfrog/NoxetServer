package org.noxet.noxetserver.menus.chat;

import com.google.common.collect.Sets;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Consumer;
import org.noxet.noxetserver.NoxetServer;
import org.noxet.noxetserver.messaging.NoxetMessage;

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
    private final BukkitTask timeout;

    public ChatPromptMenu(String message, Set<Player> promptedPlayers, Consumer<PromptResponse> callback) {
        this.message = message;
        this.promptedPlayers = promptedPlayers;
        this.callback = callback;

        NoxetServer.getPlugin().getServer().getPluginManager().registerEvents(this, NoxetServer.getPlugin());

        for(Player player : promptedPlayers)
            promptPlayer(player);

        timeout = new BukkitRunnable() {
            @Override
            public void run() {
                stop();
                for(Player player : promptedPlayers)
                    if(player.isOnline())
                        dispatchPlayerMessage(player, "");
            }
        }.runTaskLater(NoxetServer.getPlugin(), 20 * 60);
    }

    public ChatPromptMenu(String message, Player promptedPlayer, Consumer<PromptResponse> callback) {
        this(message, Sets.newHashSet(promptedPlayer), callback);
    }

    public void promptPlayer(Player player) {
        StringBuilder blocks = new StringBuilder();
        for(int i = 0; i < 40; i++)
            blocks.append("■");
        new NoxetMessage("§8" + blocks).send(player);
        new NoxetMessage("§3Enter §b" + message + "§3:").send(player);
    }

    public void dispatchPlayerMessage(Player player, String value) {
        promptedPlayers.remove(player);
        if(promptedPlayers.isEmpty())
            stop();
        callback.accept(new PromptResponse(player, value));
    }

    @EventHandler(priority = EventPriority.LOW)
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
        timeout.cancel();
    }
}
