package org.noxet.noxetserver;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.noxet.noxetserver.messaging.NoxetMessage;
import org.noxet.noxetserver.playerstate.PlayerState;
import org.noxet.noxetserver.playerstate.PlayerState.PlayerStateType;

import java.util.ArrayList;
import java.util.List;

public class RealmManager {

    /**
     * Contains all registered realms and their properties.
     */
    public enum Realm {
        SMP("SMP", PlayerStateType.SMP),
        ANARCHY("Anarchy Island", PlayerStateType.ANARCHY);

        private final String displayName;
        private final PlayerStateType playerStateType;

        Realm(String displayName, PlayerStateType playerStateType) {
            this.displayName = displayName;
            this.playerStateType = playerStateType;
        }

        public PlayerStateType getPlayerStateType() {
            return playerStateType;
        }

        public Location getSpawnLocation() {
            // To detect spawn location for a realm,
            // we loop through the server worlds
            // and find the first world that is part of this realm,
            // and get that world's spawn location.
            for(NoxetServer.ServerWorld serverWorld : NoxetServer.ServerWorld.values())
                if(serverWorld.getRealm() == this)
                    return serverWorld.getWorld().getSpawnLocation();

            throw new RuntimeException("Realm has no registered server worlds to spawn into.");
        }

        public List<World> getWorlds() {
            List<World> worlds = new ArrayList<>();
            for(NoxetServer.ServerWorld serverWorld : NoxetServer.ServerWorld.values()) // Check worlds from ServerWorld enum.
                if(serverWorld.getRealm() == this)
                    worlds.add(serverWorld.getWorld());
            return worlds;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private static final List<Player> migratingPlayers = new ArrayList<>();

    /**
     * Gets the realm that the world belongs to.
     * @param world The world whose realm to return
     * @return The realm that the world is in, null if not a realm
     */
    public static Realm getRealmFromWorld(World world) {
        for(NoxetServer.ServerWorld serverWorld : NoxetServer.ServerWorld.values())
            if(world == serverWorld.getWorld())
                return serverWorld.getRealm();

        return null;
    }

    /**
     * Gets the realm that the player is currently in.
     * @param player The player whose realm to return
     * @return The realm that the player is in, null if not in a realm
     */
    public static Realm getCurrentRealm(Player player) {
        return getRealmFromWorld(player.getWorld());
    }

    /**
     * Prepares player to move to realm by saving current state, restoring realm state and performing the teleport.
     * @param player The player to move to another realm
     * @param toRealm The realm to move the player to
     */
    public static void migrateToRealm(Player player, Realm toRealm) {
        if(migratingPlayers.contains(player))
            return;

        if(toRealm != null)
            new NoxetMessage("You are entering §e" + toRealm.getDisplayName() + "§7 ...").send(player);

        // Save state in current realm:

        Realm fromRealm = getCurrentRealm(player);

        if(toRealm == fromRealm)
            return; // Already in that realm. Do nothing.

        if(fromRealm != null) { // Source location is a realm.
            PlayerState.saveState(player, fromRealm.getPlayerStateType()); // Save state in old location's realm.
        } else {
            PlayerState.saveState(player, PlayerStateType.GLOBAL); // In non-realm world. Using global state.
        }

        // Migrate to realm:

        migratingPlayers.add(player);

        if(toRealm != null) { // Destination is a realm.
            if(!PlayerState.hasState(player, toRealm.getPlayerStateType())) // Player has no state for destination.
                player.teleport(toRealm.getSpawnLocation()); // Teleport to spawn (first join).
            PlayerState.restoreState(player, toRealm.getPlayerStateType()); // Restores player state (including initial reset), and teleports to last location (in a world belonging to the realm).
        } else {
            PlayerState.restoreState(player, PlayerStateType.GLOBAL); // Regular world. Load global state.
        }

        migratingPlayers.remove(player);

        // Say hello/goodbye to new/old realms:

        if(fromRealm != null)
            new NoxetMessage("§f" + player.getDisplayName() + "§7 left §f" + fromRealm.getDisplayName() + "§7.").send(fromRealm);

        if(toRealm != null)
            new NoxetMessage("§f" + player.getDisplayName() + "§7 joined §f" + toRealm.getDisplayName() + "§7.").send(toRealm);
    }

    /**
     * Send a player to the spawn. If in a realm, sent to realm spawn. If not in a realm, sent to world spawn.
     * @param player The player to send to spawn
     */
    public static void goToSpawn(Player player) {
        Realm realm = getCurrentRealm(player);

        player.teleport(realm != null ? realm.getSpawnLocation() : player.getWorld().getSpawnLocation());

        new NoxetMessage("You have been sent to spawn!").send(player);
    }

    /**
     * Send a player to the central hub.
     * @param player The player to send to hub
     */
    public static void goToHub(Player player) {
        player.teleport(NoxetServer.ServerWorld.HUB.getWorld().getSpawnLocation());
    }
}
