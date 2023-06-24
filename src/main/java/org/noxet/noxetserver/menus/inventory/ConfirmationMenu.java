package org.noxet.noxetserver.menus.inventory;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.scheduler.BukkitRunnable;
import org.noxet.noxetserver.NoxetServer;
import org.noxet.noxetserver.menus.ItemGenerator;
import org.noxet.noxetserver.util.TextBeautifier;
import org.noxet.noxetserver.util.InventoryCoordinate;

import java.util.Collections;

public class ConfirmationMenu extends InventoryMenu {
    private final String question;
    private final Runnable onConfirm, onCancel;
    private static final int maxConfirmClicks = 3;
    private int confirmClicksLeft = maxConfirmClicks;

    public ConfirmationMenu(String question, Runnable onConfirm, Runnable onCancel) {
        super(4, question, true);
        this.question = question;
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;
    }

    public enum ConfirmationSlot implements InventoryCoordinate {
        CONFIRM(1, 2),
        CANCEL(7, 2);

        private final int x, y;

        ConfirmationSlot(int x, int y) {
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
        setSlotItem(ItemGenerator.generateItem(
                Material.YELLOW_CONCRETE_POWDER,
                "§e" + question,
                Collections.singletonList("§7Choose an option to confirm or cancel.")), 4, 1); // Question.

        setSlotItem(ItemGenerator.generateItem(
                Material.GREEN_CONCRETE_POWDER,
                "§a" + TextBeautifier.beautify("Confirm"),
                Collections.singletonList("§e→ Double-click §3§n" + (confirmClicksLeft != 1 ? confirmClicksLeft + " times" : "once") + "§e to confirm.")),
                ConfirmationSlot.CONFIRM
        );

        setSlotItem(ItemGenerator.generateItem(
                Material.RED_CONCRETE_POWDER,
                "§c" + TextBeautifier.beautify("Cancel"),
                Collections.singletonList("§e→ Click to cancel.")),
                ConfirmationSlot.CANCEL
        );
    }

    @Override
    protected boolean onSlotClick(Player player, InventoryCoordinate coordinate, ClickType clickType) {
        if(coordinate.isAt(ConfirmationSlot.CONFIRM)) {
            if(clickType != ClickType.DOUBLE_CLICK)
                return false;

            player.playSound(player, Sound.BLOCK_BELL_USE, 1, 1.5f * confirmClicksLeft / maxConfirmClicks + 0.5f);

            if(--confirmClicksLeft != 0) {
                updateInventory();
                return false;
            }

            if(onConfirm != null)
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        onConfirm.run();
                    }
                }.runTaskLater(NoxetServer.getPlugin(), 5);
        } else if(coordinate.isAt(ConfirmationSlot.CANCEL)) {
            if(onCancel != null)
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        onCancel.run();
                    }
                }.runTaskLater(NoxetServer.getPlugin(), 5);
        } else return false;

        return true;
    }
}
