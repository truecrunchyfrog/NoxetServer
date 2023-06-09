package org.noxet.noxetserver;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.noxet.noxetserver.messaging.NoxetActionBarMessage;

import java.util.*;

import static org.noxet.noxetserver.RealmManager.getCurrentRealm;

public class CombatLogging {
    private static final Map<Player, BukkitTask> combatLogged = new HashMap<>();
    private static final int combatLogTimeout = 20 * 20;

    public static void triggerCombatLog(Player player) {
        BukkitTask existingTask = combatLogged.remove(player);
        if(existingTask != null)
            existingTask.cancel(); // If already combat logged: cancel the timeout and set a new one:
        else
            new NoxetActionBarMessage("§c§lCOMBAT LOGGED!§7 Logging out or teleporting away will kill you.").send(player);

        combatLogged.put(player, new BukkitRunnable() {
            @Override
            public void run() {
                if(combatLogged.remove(player) != null)
                    new NoxetActionBarMessage("§aYou may now leave without consequences.").send(player);
            }
        }.runTaskLater(NoxetServer.getPlugin(), combatLogTimeout));
    }

    public static boolean isCombatLogged(Player player) {
        return combatLogged.containsKey(player);
    }

    public static void triggerLocationDisband(Player player) {
        if(!isCombatLogged(player))
            return;

        Location dropAt = player.getLocation();

        if(dropAt.getWorld() == null)
            return;

        PlayerInventory playerInventory = player.getInventory();

        List<ItemStack> itemsToDrop = new ArrayList<>();

        itemsToDrop.add(playerInventory.getItemInOffHand());
        itemsToDrop.addAll(Arrays.asList(playerInventory.getContents()));
        itemsToDrop.addAll(Arrays.asList(playerInventory.getArmorContents()));

        for(ItemStack itemStack : itemsToDrop)
            dropAt.getWorld().dropItemNaturally(dropAt, itemStack);

        playerInventory.clear(); // Clear inventory to prevent player from dropping those when dying upon return.
        player.setExp(0);
        player.setLevel(0);

        playerInventory.clear();
        new CombatLoggingStorageManager().setPlayerCombatLogMode(player, getCurrentRealm(player), true);
    }
}
