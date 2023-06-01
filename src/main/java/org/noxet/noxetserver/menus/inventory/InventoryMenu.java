package org.noxet.noxetserver.menus.inventory;

import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.noxet.noxetserver.NoxetServer;

@SuppressWarnings("ALL")
public abstract class InventoryMenu implements InventoryHolder, Listener {
    private final Inventory inventory;
    private final boolean forceOpen;

    public InventoryMenu(int rows, String title, boolean forceOpen) {
        inventory = Bukkit.createInventory(this, rows * 9, title);
        this.forceOpen = forceOpen;

        NoxetServer.getPlugin().getServer().getPluginManager().registerEvents(this, NoxetServer.getPlugin());

        new BukkitRunnable() {
            @Override
            public void run() {
                updateInventory();
            }
        }.runTaskLater(NoxetServer.getPlugin(), 0);
    }

    abstract protected void updateInventory();

    abstract protected void onSlotClick(Player player, int x, int y, ClickType clickType);

    protected void setSlotItem(ItemStack itemStack, int x, int y) {
        assert x >= 0 && x < 9 && y >= 0 && y < inventory.getSize() / 9;
        inventory.setItem(y * 9 + x, itemStack);
    }

    public void openInventory(Player player) {
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if(e.getClickedInventory().equals(inventory)) {
            Player player = (Player) e.getWhoClicked();
            int slot = e.getSlot();

            onSlotClick(player, slot % 9, slot / 9, e.getClick());
        } else if(!e.getInventory().equals(inventory))
            return;

        e.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if(e.getInventory().equals(inventory)) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if(forceOpen)
                        e.getPlayer().openInventory(e.getInventory());
                    else if(inventory.getViewers().size() == 0)
                        stop();
                }
            }.runTaskLater(NoxetServer.getPlugin(), 0);
        }
    }

    protected void stop() {
        HandlerList.unregisterAll(this);

        for(HumanEntity humanEntity : inventory.getViewers())
            new BukkitRunnable() {
                @Override
                public void run() {
                    humanEntity.closeInventory();
                }
            }.runTaskLater(NoxetServer.getPlugin(), 0);
    }
}
