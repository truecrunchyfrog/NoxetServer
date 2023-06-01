package org.noxet.noxetserver.menus.inventory;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.scheduler.BukkitRunnable;
import org.noxet.noxetserver.NoxetServer;
import org.noxet.noxetserver.menus.ItemGenerator;
import org.noxet.noxetserver.messaging.TextBeautifier;

import java.util.Collections;

public class ConfirmMenu extends InventoryMenu {
    private final String question;
    private final Runnable onConfirm, onCancel;
    private int confirmClicksLeft = 3;

    public ConfirmMenu(String question, Runnable onConfirm, Runnable onCancel) {
        super(4, question, true);
        this.question = question;
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;
    }

    @Override
    protected void updateInventory() {
        setSlotItem(ItemGenerator.generateItem(
                Material.YELLOW_CONCRETE_POWDER,
                1,
                "§e" + question,
                Collections.singletonList("§7Choose an option to confirm or cancel."), true), 4, 1); // Question.

        setSlotItem(ItemGenerator.generateItem(
                Material.GREEN_CONCRETE_POWDER,
                "§a" + TextBeautifier.beautify("Confirm"),
                Collections.singletonList("§eRight-click " + (confirmClicksLeft != 1 ? confirmClicksLeft + " times" : "once") + " to confirm.")), 1, 2); // Confirm.

        setSlotItem(ItemGenerator.generateItem(
                Material.RED_CONCRETE_POWDER,
                "§c" + TextBeautifier.beautify("Cancel")), 7, 2); // Cancel.
    }

    @Override
    protected void onSlotClick(Player player, int x, int y, ClickType clickType) {
        if(y == 2) {
            switch(x) {
                case 1: // Confirm
                    if(!clickType.isRightClick())
                        break;
                    if(--confirmClicksLeft == 0) {
                        stop();
                        if(onConfirm != null)
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    onConfirm.run();
                                }
                            }.runTaskLater(NoxetServer.getPlugin(), 0);
                    } else
                        updateInventory();
                    break;
                case 7: // Cancel
                    stop();
                    if(onCancel != null)
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                onCancel.run();
                            }
                        }.runTaskLater(NoxetServer.getPlugin(), 0);
                    break;
            }
        }
    }
}
