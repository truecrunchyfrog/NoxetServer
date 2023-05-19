package org.noxet.noxetserver;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.noxet.noxetserver.messaging.NoxetMessage;
import org.noxet.noxetserver.playerstate.PlayerState;
import org.noxet.noxetserver.playerstate.PlayerState.PlayerStateType;

import java.util.Collections;
import java.util.List;

public class RealmManager {

    /**
     * Contains all registered realms and their properties.
     */
    public enum Realm {
        SMP("SMP", PlayerStateType.SMP, NoxetServer.ServerWorld.SMP_SPAWN.getWorld().getSpawnLocation()),
        ANARCHY("Anarchy Islands", PlayerStateType.ANARCHY, NoxetServer.ServerWorld.ANARCHY_WORLD.getWorld().getSpawnLocation());

        private final String displayName;
        private final PlayerStateType playerStateType;
        private final Location spawnLocation;

        Realm(String displayName, PlayerStateType playerStateType, Location spawnLocation) {
            this.displayName = displayName;
            this.playerStateType = playerStateType;
            this.spawnLocation = spawnLocation;
        }

        public PlayerStateType getPlayerStateType() {
            return playerStateType;
        }

        public Location getSpawnLocation() {
            return spawnLocation;
        }

        public List<World> getWorlds() {
            List<World> worlds = Collections.emptyList();
            for(NoxetServer.ServerWorld serverWorld : NoxetServer.ServerWorld.values()) // Check worlds from ServerWorld enum.
                if(serverWorld.getRealm() == this)
                    worlds.add(serverWorld.getWorld());
            return worlds;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Gets the realm that the player is currently in.
     * @param player The player whose realm to return
     * @return The realm that the player is in, returns null if not in a realm
     */
    public static Realm getCurrentRealm(Player player) {
        for(NoxetServer.ServerWorld serverWorld : NoxetServer.ServerWorld.values())
            if(player.getWorld() == serverWorld.getWorld())
                return serverWorld.getRealm();

        return null;
    }

    /**
     * Prepares player to move to realm by saving current state, restoring realm state and performing the teleport.
     * @param player The player to move to another realm
     * @param toRealm The realm to move the player to
     */
    public static void migrateToRealm(Player player, Realm toRealm) {
        new NoxetMessage("You are entering §e" + toRealm.getDisplayName() + "§7 ...").send(player);

        // Save state in current realm:

        Realm fromRealm = getCurrentRealm(player);

        if(toRealm == fromRealm)
            return; // Already in that realm. Do nothing.

        if(fromRealm != null) {
            PlayerState.saveState(player, fromRealm.getPlayerStateType());
        } else {
            PlayerState.saveState(player, PlayerStateType.GLOBAL); // In non-realm world. Using global state.
        }

        // Migrate to realm:

        if(toRealm != null) {
            if(!PlayerState.hasState(player, toRealm.getPlayerStateType()))
                player.teleport(toRealm.getSpawnLocation());
            PlayerState.restoreState(player, toRealm.getPlayerStateType()); // Restores player state (including initial reset), and teleports to last location (in a world belonging to the realm).
        } else {
            PlayerState.restoreState(player, PlayerStateType.GLOBAL); // Reset player if moving from realm to regular world.
        }

        if(fromRealm != null)
            new NoxetMessage("§f" + player.getDisplayName() + "§7 left §f" + fromRealm.getDisplayName() + "§7.").send(fromRealm);

        if(toRealm != null)
            new NoxetMessage("§f" + player.getDisplayName() + "§7 joined §f" + toRealm.getDisplayName() + "§7.").send(toRealm);
    }

    /**
     * Send a player to the spawn in the current realm.
     * @param player The player to send to spawn
     */
    public static void goToSpawn(Player player) {
        player.teleport(getCurrentRealm(player).getSpawnLocation());
        new NoxetMessage("You have been sent to spawn!").send(player);
    }

    /**
     * Send a player to the central hub.
     * @param player The player to send to hub
     */
    public static void goToHub(Player player) {
        if(player.getWorld() != NoxetServer.ServerWorld.HUB.getWorld()) {
            new NoxetMessage("You are being sent to hub.").send(player);
            migrateToRealm(player, null);
        }

        player.teleport(NoxetServer.ServerWorld.HUB.getWorld().getSpawnLocation());
    }
}
