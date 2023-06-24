package org.noxet.noxetserver.messaging;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.noxet.noxetserver.NoxetServer;
import org.noxet.noxetserver.RealmManager;
import org.noxet.noxetserver.messaging.channels.MessagingChannel;
import org.noxet.noxetserver.util.TextBeautifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Message {
    private final List<TextComponent> textComponents = new ArrayList<>();
    private String prefix = getDefaultPrefix();
    protected ChatMessageType chatMessageType = ChatMessageType.CHAT;

    /**
     * Constructs a message with a text.
     * @param text The text message to be sent
     */
    public Message(String text) {
        add(text, null, null);
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String newPrefix) {
        prefix = newPrefix;
    }

    public static String getDefaultPrefix() {
        return "§b" + TextBeautifier.beautify("no") + "§3§lx§b" + TextBeautifier.beautify("et") + " §8§l| ";
    }

    protected TextComponent getBakedMessage() {
        TextComponent mainComponent = new TextComponent(prefix != null ? prefix : "");

        for(TextComponent textComponent : textComponents)
            mainComponent.addExtra(textComponent); // Add every component.

        return mainComponent;
    }

    public ChatColor getDefaultColor() {
        return ChatColor.GRAY;
    }

    public Message add(String text, String hoverText, String clickCommand) {
        if(text == null)
            return this;

        TextComponent textComponent = new TextComponent(text + "§r "); // Padding, for better transitions.

        textComponent.setColor(getDefaultColor());

        if(hoverText != null)
            textComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(hoverText)));

        if(clickCommand != null)
            textComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + clickCommand));

        textComponents.add(textComponent);

        return this;
    }

    public Message add(String text, String hoverText) {
        return add(text, hoverText, null);
    }

    public Message add(String text) {
        return add(text, null, null);
    }

    public Message addButton(String buttonLabel, ChatColor color, String hoverText, String clickCommand) {
        TextComponent textComponent = new TextComponent("→" + TextBeautifier.beautify(buttonLabel) + " ");

        textComponent.setColor(color);
        textComponent.setBold(true);

        if(hoverText != null)
            textComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(hoverText)));

        if(clickCommand != null)
            textComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + clickCommand));

        textComponents.add(textComponent);

        return this;
    }

    public void send(Player player) {
        player.spigot().sendMessage(chatMessageType, getBakedMessage());
    }

    public void send(CommandSender commandSender) {
        commandSender.spigot().sendMessage(getBakedMessage());
    }

    public void send(Collection<? extends Player> players) {
        for(Player player : players)
            send(player);
    }

    public void send(World world) {
        send(world.getPlayers());
    }

    public void send(World[] worlds) {
        for(World world : worlds)
            send(world);
    }

    public void send(RealmManager.Realm realm) {
        if(realm != null)
            send(realm.getWorlds().toArray(new World[0]));
        else
            send(NoxetServer.ServerWorld.HUB.getWorld());
    }

    public void send(MessagingChannel channel) {
        for(CommandSender recipient : channel.getRecipients())
            send(recipient);
    }

    public void broadcast() {
        Collection<? extends Player> sendTo = NoxetServer.getPlugin().getServer().getOnlinePlayers();
        send(sendTo);
    }
}