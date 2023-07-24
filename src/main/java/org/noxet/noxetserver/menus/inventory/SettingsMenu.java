package org.noxet.noxetserver.menus.inventory;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.noxet.noxetserver.menus.ItemGenerator;
import org.noxet.noxetserver.menus.chat.ChatPromptMenu;
import org.noxet.noxetserver.messaging.ErrorMessage;
import org.noxet.noxetserver.util.InventoryCoordinate;
import org.noxet.noxetserver.util.PlayerDataEraser;

import java.util.Arrays;

public class SettingsMenu extends InventoryMenu {
    public SettingsMenu() {
        super(3, "Settings", false);
    }

    public enum SettingSlot implements InventoryCoordinate {
        ERASE_USER_DATA(8, 2);

        private final int x, y;

        SettingSlot(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public int getX() {
            return x;
        }

        @Override
        public int getY() {
            return y;
        }
    }

    @Override
    protected void updateInventory() {
        setSlotItem(
                ItemGenerator.generateItem(
                        Material.SKELETON_SKULL,
                        "§4Erase your data",
                        Arrays.asList(
                                "§7Any data saved on your",
                                "§7account will be deleted.",
                                "§6§lIRREVERSIBLE!",
                                "§eConsider carefully before",
                                "§eerasing your data."
                        )
                ), SettingSlot.ERASE_USER_DATA);
    }

    @Override
    protected boolean onSlotClick(Player player, InventoryCoordinate coordinate, ClickType clickType) {
        if(coordinate.isAt(SettingSlot.ERASE_USER_DATA)) {
            String confirmText = "I " + player.getName() + " HEREBY RESIGN";
            new ChatPromptMenu("the message: \"" + confirmText + "\", to have your account data on Noxet deleted in 3 days (cancel any time)", player, promptResponse -> {
                if(promptResponse.getMessage().equalsIgnoreCase(confirmText)) {
                    new PlayerDataEraser().planDataErasure(player.getUniqueId());
                    player.kickPlayer("§cGoodbye, " + player.getName() + "...\n\nYou have requested erasure of your data.\nEverything linked to your account (except e.g. in-game signs, world and chest items) will be removed in 3 days.\nLog in within this time to abort the removal.");
                    return;
                }

                new ErrorMessage(ErrorMessage.ErrorType.COMMON, "Wrong message entered. Nothing happened.").send(player);
            });
            return true;
        }

        return false;
    }
}
