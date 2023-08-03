package org.noxet.noxetserver.realm;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.noxet.noxetserver.combatlogging.CombatLoggingStorageManager;
import org.noxet.noxetserver.Events;
import org.noxet.noxetserver.NoxetServer;
import org.noxet.noxetserver.commands.teleportation.TeleportAsk;
import org.noxet.noxetserver.menus.book.BookMenu;
import org.noxet.noxetserver.messaging.ActionBarMessage;
import org.noxet.noxetserver.messaging.Message;
import org.noxet.noxetserver.minigames.MiniGameManager;
import org.noxet.noxetserver.util.TextBeautifier;
import org.noxet.noxetserver.playerdata.PlayerDataManager;
import org.noxet.noxetserver.playerstate.PlayerState;
import org.noxet.noxetserver.playerstate.PlayerState.PlayerStateType;

import java.util.*;
import java.util.function.Consumer;

public class RealmManager {

    /**
     * Contains all registered realms and their properties.
     */
    public enum Realm {
        SMP("SMP", PlayerStateType.SMP, true, null, null),
        ANARCHY("Anarchy Island", PlayerStateType.ANARCHY, false, player -> {
            if(!hasPlayerUnderstoodAnarchy(player)) {
                Realm realmBefore = getCurrentRealm(player); // We know this is Realm.ANARCHY, but it is not yet defined in this scope.

                BookMenu bookMenu = new BookMenu(Collections.singletonList(
                        new ComponentBuilder(
                                "§8Cheats are allowed in the §cAnarchy Island§8 realm and §lONLY§8 there!\n" +
                                        "Using cheats outside of this realm will get you banned.\n" +
                                        "§cMalicious cheats (that produce lag, spam, etc.) are not allowed.\n\n")
                                .append(new ComponentBuilder("§2§l■ Thanks, I'll remember this.").event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, Events.TemporaryCommand.UNDERSTAND_ANARCHY.getSlashCommand())).event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Close this warning"))).create()).create()
                ));

                Events.setTemporaryInvulnerability(player, 25);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if(
                                player.isOnline() &&
                                realmBefore.equals(getCurrentRealm(player)) &&
                                !hasPlayerUnderstoodAnarchy(player)
                        )
                            bookMenu.openMenu(player);
                        else
                            this.cancel();
                    }
                }.runTaskTimer(NoxetServer.getPlugin(), 20, 20);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if(
                            player.isOnline() &&
                            realmBefore.equals(getCurrentRealm(player)) &&
                            !hasPlayerUnderstoodAnarchy(player)
                        )
                            player.kickPlayer("§cYou did not confirm.");
                    }
                }.runTaskLater(NoxetServer.getPlugin(), 20 * 25);
            }
        }, player -> {
            if(MiniGameManager.isPlayerBusyInGame(player))
                return;

            Events.setPlayerRecentlyKickedToRemoveCheats(player.getUniqueId());

            new BukkitRunnable() {
                @Override
                public void run() {
                    player.kickPlayer(
                            "§a☺ This is just a helping friendly reminder. You are §nnot§a banned.\n\n" +
                                    "§7⚠ §lEXITING ANARCHISTIC REGION §7⚠\n" +
                                    "§c" + TextBeautifier.beautify("You were disconnected because you left Anarchy Island.") + "\n" +
                                    TextBeautifier.beautify("Cheats are not allowed where you are heading.") + "\n" +
                                    "§e" + TextBeautifier.beautify("Close any advantage-granting modifications before rejoining our server, to avoid a ban.")
                    );
                }
            }.runTaskLater(NoxetServer.getPlugin(), 20);
        }),
        CANVAS("Canvas", PlayerStateType.CANVAS, true, null, null);

        private final String displayName;
        private final PlayerStateType playerStateType;
        private final boolean allowTeleportationMethods;
        private final Consumer<Player> onMigrateToCallback, onMigrateFromCallback;

        Realm(String displayName, PlayerStateType playerStateType, boolean allowTeleportationMethods, Consumer<Player> onMigrateToCallback, Consumer<Player> onMigrateFromCallback) {
            this.displayName = displayName;
            this.playerStateType = playerStateType;
            this.allowTeleportationMethods = allowTeleportationMethods;
            this.onMigrateToCallback = onMigrateToCallback;
            this.onMigrateFromCallback = onMigrateFromCallback;
        }

        public PlayerStateType getPlayerStateType() {
            return playerStateType;
        }

        public World getWorld(NoxetServer.WorldFlag worldFlag) {
            for(NoxetServer.ServerWorld serverWorld : NoxetServer.ServerWorld.values())
                if(serverWorld.getRealm() == this && serverWorld.getWorldFlag() == worldFlag)
                    return serverWorld.getWorld();

            return null;
        }

        public Location getSpawnLocation() {
            Location tailoredLocation = new RealmDataManager().getSpawnLocation(this);
            if(tailoredLocation != null)
                return tailoredLocation;

            World neutralWorld = getWorld(NoxetServer.WorldFlag.NEUTRAL);
            if(neutralWorld != null)
                return neutralWorld.getSpawnLocation();

            World overWorld = getWorld(NoxetServer.WorldFlag.OVERWORLD);
            if(overWorld != null)
                return overWorld.getSpawnLocation();

            return null;
        }

        public boolean doesAllowTeleportationMethods() {
            return allowTeleportationMethods;
        }

        public List<World> getWorlds() {
            List<World> worlds = new ArrayList<>();
            for(NoxetServer.ServerWorld serverWorld : NoxetServer.ServerWorld.values()) // Check worlds from ServerWorld enum.
                if(serverWorld.getRealm() == this)
                    worlds.add(serverWorld.getWorld());
            return worlds;
        }

        public Set<Player> getPlayers() {
            Set<Player> players = new HashSet<>();

            for(World world : getWorlds())
                players.addAll(world.getPlayers());

            return players;
        }

        public void onMigration(Player player, boolean to) {
            if(to && onMigrateToCallback != null)
                onMigrateToCallback.accept(player);
            else if(!to && onMigrateFromCallback != null)
                onMigrateFromCallback.accept(player);
        }

        public int getPlayerCount() {
            return getPlayers().size();
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private static final Set<Player> migratingPlayers = new HashSet<>();

    public static void setPlayerMigrationStatus(Player player, boolean migrating) {
        if(migrating)
            migratingPlayers.add(player);
        else
            migratingPlayers.remove(player);
    }

    public static boolean isPlayerMigrating(Player player) {
        return migratingPlayers.contains(player);
    }

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
        if(isPlayerMigrating(player))
            return;

        // Save state in current realm:

        Realm fromRealm = getCurrentRealm(player);

        if(toRealm == fromRealm)
            return; // Already in that realm. Do nothing.

        TeleportAsk.abortPlayerRelatedRequests(player);
        Events.abortUnconfirmedPlayerRespawn(player);
        new CombatLoggingStorageManager().combatLogRejoin(player, getCurrentRealm(player));

        if(toRealm != null)
            new Message("Entering §e" + toRealm.getDisplayName() + "§7 ...").send(player);

        if(fromRealm != null) { // Source location is a realm.
            PlayerState.saveState(player, fromRealm.getPlayerStateType()); // Save state in old location's realm.
            fromRealm.onMigration(player, false);
        } else {
            PlayerState.saveState(player, PlayerStateType.GLOBAL); // In non-realm world. Using global state.
        }

        // Migrate to realm:

        setPlayerMigrationStatus(player, true);

        new BukkitRunnable() {
            @Override
            public void run() {
                setPlayerMigrationStatus(player, false);
            }
        }.runTaskLater(NoxetServer.getPlugin(), 60); // 3 seconds of migration margin.

        if(toRealm != null) { // Destination is a realm.
            Events.setTemporaryInvulnerability(player);

            PlayerState.restoreState(player, toRealm.getPlayerStateType()); // Restores player state (including initial reset), and teleports to last location (in a world belonging to the realm).

            if(!PlayerState.hasState(player, toRealm.getPlayerStateType()) && toRealm.getSpawnLocation() != null)
                player.teleport(toRealm.getSpawnLocation()); // Teleport to spawn (first join).

            toRealm.onMigration(player, true);
        } else {
            PlayerState.restoreState(player, PlayerStateType.GLOBAL); // Regular world. Load global state.
        }

        setPlayerMigrationStatus(player, false);

        Events.updatePlayerListName(player);

        // Say hello/goodbye to new/old realms:

        if(fromRealm != null)
            new Message("§f" + player.getDisplayName() + "§7 left §f" + fromRealm.getDisplayName() + "§7.").send(fromRealm);

        if(toRealm != null)
            new Message("§f" + player.getDisplayName() + "§7 joined §f" + toRealm.getDisplayName() + "§7.").send(toRealm);
    }

    public static Location getMainSpawn() {
        Location preferred = new RealmDataManager().getSpawnLocation(null);
        return preferred != null ? preferred : NoxetServer.ServerWorld.HUB.getWorld().getSpawnLocation();
    }

    public static Location getSpawnLocation(Player player) {
        Realm realm = getCurrentRealm(player);

        return realm != null ?
                realm.getSpawnLocation() :
                getMainSpawn();
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

        new ActionBarMessage("§7You have been sent to " + (realm != null ? "§f§l" + realm.getDisplayName() + "§7 " : "") + "spawn!").send(player);
    }

    /**
     * Send a player to the central hub.
     * @param player The player to send to hub
     */
    public static void goToHub(Player player) {
        PlayerState.prepareHubState(player);
        player.teleport(getMainSpawn());

        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_FLUTE, 1, 0.5f);
        player.sendTitle("§b§l" + TextBeautifier.beautify("no") + "§3§l" + TextBeautifier.beautify("x") + "§b§l" + TextBeautifier.beautify("et"), "§eWelcome to the Noxet.org Network.", 0, 60, 5);

        player.spawnParticle(Particle.EXPLOSION_HUGE, player.getLocation().add(player.getLocation().getDirection().multiply(2)).add(0, 1, 0), 10);
    }

    public static boolean hasPlayerUnderstoodAnarchy(Player player) {
        return (boolean) new PlayerDataManager(player).get(PlayerDataManager.Attribute.HAS_UNDERSTOOD_ANARCHY);
    }
}
