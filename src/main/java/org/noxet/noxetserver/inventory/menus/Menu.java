package org.noxet.noxetserver.inventory.menus;

import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.noxet.noxetserver.NoxetServer;
import org.noxet.noxetserver.messaging.NoxetMessage;

public abstract class Menu implements InventoryHolder, Listener {
    private final Inventory inventory;
    private final boolean forceOpen;

    public Menu(int rows, String title, boolean forceOpen) {
        inventory = Bukkit.createInventory(this, rows * 9, title);
        this.forceOpen = forceOpen;

        NoxetServer.getPlugin().getServer().getPluginManager().registerEvents(this, NoxetServer.getPlugin());

        updateInventory();
    }

    abstract protected void updateInventory();

    abstract protected void onSlotClick(Player player, int x, int y);

    protected void setSlotItem(ItemStack itemStack, int x, int y) {
        assert x >= 0 && x < 9 && y >= 0 && y < inventory.getSize() / 9;
        inventory.setItem(y * 9 + x, itemStack);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if(e.getInventory().equals(inventory)) {
            Player player = (Player) e.getWhoClicked();
            int slot = e.getSlot();

            onSlotClick(player, slot % 9, slot / 9);

            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if(e.getInventory().equals(inventory)) {
            if(forceOpen)
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        e.getPlayer().openInventory(e.getInventory());
                    }
                }.runTaskLater(NoxetServer.getPlugin(), 5);
            else
                stop();
        }
    }

    private void stop() {
        HandlerList.unregisterAll(this);

        for(HumanEntity humanEntity : inventory.getViewers())
            humanEntity.closeInventory();
    }
}
