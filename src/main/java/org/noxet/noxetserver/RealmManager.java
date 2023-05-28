package org.noxet.noxetserver;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.noxet.noxetserver.commands.teleportation.TeleportAsk;
import org.noxet.noxetserver.messaging.NoxetMessage;
import org.noxet.noxetserver.messaging.TextBeautifier;
import org.noxet.noxetserver.playerstate.PlayerState;
import org.noxet.noxetserver.playerstate.PlayerState.PlayerStateType;

import java.util.ArrayList;
import java.util.List;

public class RealmManager {

    /**
     * Contains all registered realms and their properties.
     */
    public enum Realm {
        SMP("SMP", PlayerStateType.SMP, true),
        ANARCHY("Anarchy Island", PlayerStateType.ANARCHY, false);

        private final String displayName;
        private final PlayerStateType playerStateType;
        private final boolean allowSpawnCommand;

        Realm(String displayName, PlayerStateType playerStateType, boolean allowSpawnCommand) {
            this.displayName = displayName;
            this.playerStateType = playerStateType;
            this.allowSpawnCommand = allowSpawnCommand;
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

            throw new RuntimeException("Realm has no registered server world to spawn into.");
        }

        public boolean doesAllowSpawnCommand() {
            return allowSpawnCommand;
        }

        public List<World> getWorlds() {
            List<World> worlds = new ArrayList<>();
            for(NoxetServer.ServerWorld serverWorld : NoxetServer.ServerWorld.values()) // Check worlds from ServerWorld enum.
                if(serverWorld.getRealm() == this)
                    worlds.add(serverWorld.getWorld());
            return worlds;
        }

        public List<Player> getPlayers() {
            List<Player> players = new ArrayList<>();

            for(World world : getWorlds())
                players.addAll(world.getPlayers());

            return players;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    protected static final List<Player> migratingPlayers = new ArrayList<>();

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

        // Save state in current realm:

        Realm fromRealm = getCurrentRealm(player);

        if(toRealm == fromRealm)
            return; // Already in that realm. Do nothing.

        TeleportAsk.abortPlayerRelatedRequests(player);

        if(toRealm != null)
            new NoxetMessage("Joining §e" + toRealm.getDisplayName() + "§7 ...").send(player);

        if(fromRealm != null) { // Source location is a realm.
            PlayerState.saveState(player, fromRealm.getPlayerStateType()); // Save state in old location's realm.
        } else {
            PlayerState.saveState(player, PlayerStateType.GLOBAL); // In non-realm world. Using global state.
        }

        // Migrate to realm:

        migratingPlayers.add(player);

        new BukkitRunnable() {
            @Override
            public void run() {
                migratingPlayers.remove(player);
            }
        }.runTaskLater(NoxetServer.getPlugin(), 60); // 3 seconds of migration margin.

        if(toRealm != null) { // Destination is a realm.
            boolean teleportToSpawn = !PlayerState.hasState(player, toRealm.getPlayerStateType());

            PlayerState.restoreState(player, toRealm.getPlayerStateType()); // Restores player state (including initial reset), and teleports to last location (in a world belonging to the realm).

            if(teleportToSpawn)
                player.teleport(toRealm.getSpawnLocation()); // Teleport to spawn (first join).
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

    public static Location getSpawnLocation(Player player) {
        Realm realm = getCurrentRealm(player);

        return realm != null ? realm.getSpawnLocation() : player.getWorld().getSpawnLocation();
    }

    public static Location getRespawnLocation(Player player) {
        Location bed = player.getBedSpawnLocation();

        return bed != null ? bed : getSpawnLocation(player);
    }

    /**
     * Send a player to the spawn. If in a realm, sent to realm spawn. If not in a realm, sent to world spawn.
     * @param player The player to send to spawn
     */
    public static void goToSpawn(Player player) {
        player.teleport(getSpawnLocation(player));

        Realm realm = getCurrentRealm(player);

        new NoxetMessage("You have been sent to " + (realm != null ? "§f§l" + realm.getDisplayName() + "§7 " : "") + "spawn!").send(player);
    }

    /**
     * Send a player to the central hub.
     * @param player The player to send to hub
     */
    public static void goToHub(Player player) {
        player.teleport(NoxetServer.ServerWorld.HUB.getWorld().getSpawnLocation());

        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_FLUTE, 1, 0.5f);
        player.sendTitle("§b§l" + TextBeautifier.beautify("no") + "§3§l" + TextBeautifier.beautify("x") + "§b§l" + TextBeautifier.beautify("et"), "§eWelcome to the Noxet.org Network.", 0, 60, 5);

        player.spawnParticle(Particle.EXPLOSION_HUGE, player.getLocation().add(player.getLocation().getDirection().multiply(2)).add(0, 1, 0), 10);

        PlayerState.prepareHubState(player);
    }
}
