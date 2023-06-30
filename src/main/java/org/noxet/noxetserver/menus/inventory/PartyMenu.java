package org.noxet.noxetserver.menus.inventory;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.noxet.noxetserver.menus.ItemGenerator;
import org.noxet.noxetserver.menus.chat.ChatPromptMenu;
import org.noxet.noxetserver.minigames.party.Party;
import org.noxet.noxetserver.util.InventoryCoordinate;
import org.noxet.noxetserver.util.InventoryCoordinateUtil;
import org.noxet.noxetserver.util.TextBeautifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PartyMenu extends InventoryMenu {
    private final Player player;
    private final Party party;
    private final InventoryCoordinate addPlayerSlot, leavePartySlot;

    public PartyMenu(Player player) {
        super(Party.isPlayerMemberOfParty(player) ? (Party.getPartyFromMember(player).getMembers().size() - 1) / 9 + 2 : 1, "Party", false);
        this.player = player;
        party = Party.getPartyFromMember(player);

        int extrasY = getInventory().getSize() / 9 - 1;
        addPlayerSlot = InventoryCoordinateUtil.getCoordinateFromXY(0, extrasY);
        leavePartySlot = InventoryCoordinateUtil.getCoordinateFromXY(8, extrasY);
    }

    @Override
    protected void updateInventory() {
        if(party == null) {
            setSlotItem(
                    ItemGenerator.generateItem(
                            Material.BLUE_WOOL,
                            "§9Create a Party!",
                            Arrays.asList("§7Play with a chosen", "§7group of players.")
                    ), 0, 0
            );
            return;
        }

        int i = 0;
        for(Player member : party.getMembers()) {
            List<String> playerDescription = new ArrayList<>();

            playerDescription.add(
                    party.isOwner(member) ?
                            "§9§l" + TextBeautifier.beautify("owner") :
                            "§7§l" + TextBeautifier.beautify("member")
            );

            if(party.isOwner(player) && member != player)
                playerDescription.addAll(Arrays.asList(
                        "§e→ Double-click to §c§nkick§e.",
                        "§e→ Shift-right-click to §9§nmake owner§e."
                ));

            setSlotItem(
                    ItemGenerator.generatePlayerSkull(
                            member,
                            "§e" + member.getName(),
                            playerDescription
                    ), InventoryCoordinateUtil.getCoordinateFromSlotIndex(i++)
            );
        }

        if(party.isOwner(player)) {
            setSlotItem(
                    ItemGenerator.generateItem(
                            Material.PAPER,
                            "§aInvite Player",
                            Arrays.asList("§7Invite a player to", "§7your party.")
                    ), addPlayerSlot
            );

            setSlotItem(
                    ItemGenerator.generateItem(
                            Material.RED_WOOL,
                            "§cDisband Party",
                            Collections.singletonList("§7Give up on this party.")
                    ), leavePartySlot
            );
        } else {
            setSlotItem(
                    ItemGenerator.generateItem(
                            Material.RED_WOOL,
                            "§cLeave Party",
                            Collections.singletonList("§7Leave " + party.getOwner().getName() + "'s party.")
                    ), leavePartySlot
            );
        }
    }

    @Override
    protected boolean onSlotClick(Player player, InventoryCoordinate coordinate, ClickType clickType) {
        if(party == null) {
            if(coordinate.isAt(InventoryCoordinateUtil.getCoordinateFromSlotIndex(0))) {
                player.performCommand("party create");
                new PartyMenu(player).openInventory(player);
                return true;
            }
            return false;
        }

        if(!party.isOwner(player)) {
            if(coordinate.isAt(leavePartySlot)) {
                player.performCommand("party leave");
                return true;
            }
            return false;
        }

        if(coordinate.isAt(addPlayerSlot)) {
            new ChatPromptMenu("player to invite", player, promptResponse -> {
                player.performCommand("party invite " + promptResponse.getMessage());
                new PartyMenu(player).openInventory(player);
            });

            return true;
        } else if(coordinate.isAt(leavePartySlot)) {
            new ConfirmationMenu("Disband party?", () -> player.performCommand("party disband"), () -> new PartyMenu(player).openInventory(player)).openInventory(player);
            return true;
        }

        if(coordinate.getSlotIndex() > party.getMembers().size() - 1)
            return false;

        int i = 0;

        for(Player member : party.getMembers())
            if(coordinate.getSlotIndex() == i++) {
                switch(clickType) {
                    case DOUBLE_CLICK:
                        player.performCommand("party kick " + member.getName());
                        break;
                    case SHIFT_RIGHT:
                        player.performCommand("party transfer " + member.getName());
                        break;
                }

                return false;
            }

        return false;
    }
}
