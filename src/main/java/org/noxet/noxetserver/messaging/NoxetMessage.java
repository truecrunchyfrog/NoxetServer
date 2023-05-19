package org.noxet.noxetserver.messaging;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.noxet.noxetserver.RealmManager;

public class NoxetMessage {
    private final String text;
    private final String clickCommand;


    /**
     * Constructs a message.
     * @param text The text message to be sent
     */
    public NoxetMessage(String text) {
        this(text, null);
    }

    /**
     * Constructs a message with a click command.
     * @param text The text message to be sent
     * @param clickCommand The command to be run when clicked
     */
    public NoxetMessage(String text, String clickCommand) {
        this.text = text;
        this.clickCommand = clickCommand;
    }

    public String getMessagePrefix() {
        return "§8§l[ §bno§3x§bet §8§l] §7";
    }

    private String getBakedMessage() {
        return getMessagePrefix() + text;
    }

    private TextComponent asTextComponent() {
        TextComponent textComponent = new TextComponent(getBakedMessage());

        if(clickCommand != null)
            textComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, clickCommand));

        return textComponent;
    }

    public void send(Player player) {
        player.spigot().sendMessage(asTextComponent());
    }

    public void send(CommandSender commandSender) {
        commandSender.spigot().sendMessage(asTextComponent());
    }

    public void send(Player[] players) {
        for(Player player : players)
            send(player);
    }

    public void send(World world) {
        send(world.getPlayers().toArray(new Player[0]));
    }

    public void send(World[] worlds) {
        for(World world : worlds)
            send(world);
    }

    public void send(RealmManager.Realm realm) {
        send(realm.getWorlds().toArray(new World[0]));
    }
}