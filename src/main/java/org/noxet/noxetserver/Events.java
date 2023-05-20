package org.noxet.noxetserver;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import static org.noxet.noxetserver.RealmManager.*;

public class Events implements Listener {
    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent e) {
        if(e.getTo() != null && e.getFrom().getWorld() != e.getTo().getWorld()) { // Teleporting to another world.
            RealmManager.Realm toRealm = getRealmFromWorld(e.getTo().getWorld());

            migrateToRealm(e.getPlayer(), toRealm); // Migrator will send the player to spawn or last location in realm.

            // Don't cancel the teleportation!
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        goToHub(e.getPlayer());
    }
}
