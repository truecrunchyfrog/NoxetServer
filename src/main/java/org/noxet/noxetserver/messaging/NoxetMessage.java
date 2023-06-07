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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class NoxetMessage {
    private final List<TextComponent> textComponents = new ArrayList<>();
    private boolean skipPrefix = false;
    private ChatMessageType chatMessageType = ChatMessageType.CHAT;

    /**
     * Constructs a message.
     */
    public NoxetMessage() {

    }

    /**
     * Constructs a message with a text.
     * @param text The text message to be sent
     */
    public NoxetMessage(String text) {
        add(text, null, null);
    }

    public String getMessagePrefix() {
        return "§b" + TextBeautifier.beautify("no") + "§3§lx§b" + TextBeautifier.beautify("et") + " §8§l| ";
    }

    private TextComponent getBakedMessage() {
        TextComponent mainComponent = new TextComponent(!skipPrefix ? getMessagePrefix() : "");

        for(TextComponent textComponent : textComponents)
            mainComponent.addExtra(textComponent); // Add every component.

        return mainComponent;
    }

    public ChatColor getDefaultColor() {
        return ChatColor.GRAY;
    }

    public NoxetMessage add(String text, String hoverText, String clickCommand) {
        TextComponent textComponent = new TextComponent(text + "§r "); // Padding, for better transitions.

        textComponent.setColor(getDefaultColor());

        if(hoverText != null)
            textComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(hoverText)));

        if(clickCommand != null)
            textComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + clickCommand));

        textComponents.add(textComponent);

        return this;
    }

    public NoxetMessage add(String text, String hoverText) {
        return add(text, hoverText, null);
    }

    public NoxetMessage add(String text) {
        return add(text, null, null);
    }

    public NoxetMessage addButton(String buttonLabel, ChatColor color, String hoverText, String clickCommand) {
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

    public void skipPrefix() {
        skipPrefix = true;
    }

    public NoxetMessage toActionBar() {
        chatMessageType = ChatMessageType.ACTION_BAR;
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

    public void broadcast() {
        Collection<? extends Player> sendTo = NoxetServer.getPlugin().getServer().getOnlinePlayers();
        send(sendTo);
    }
}