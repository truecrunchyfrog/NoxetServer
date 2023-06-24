package org.noxet.noxetserver.menus.inventory;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Consumer;
import org.noxet.noxetserver.NoxetServer;
import org.noxet.noxetserver.menus.ItemGenerator;
import org.noxet.noxetserver.minigames.MiniGameController;
import org.noxet.noxetserver.minigames.worldeater.WorldEater;
import org.noxet.noxetserver.util.InventoryCoordinate;
import org.noxet.noxetserver.util.InventoryCoordinateUtil;

import java.util.*;

public class WorldEaterTeamSelectionMenu extends InventoryMenu {
    private final MiniGameController game;
    private final Set<Player> seekers, hiders, readyPlayers;

    private final BukkitTask timer;
    private int timeLeft;

    private final Consumer<WorldEaterTeamSelectionMenu> callback;

    private final InventoryCoordinate timeSlot, readySlot;

    public WorldEaterTeamSelectionMenu(WorldEater game, int secondsToWait, Consumer<WorldEaterTeamSelectionMenu> callback) {
        super(3, "Choose a Team", true);

        this.game = game;

        seekers = new HashSet<>();
        hiders = new HashSet<>();
        readyPlayers = new HashSet<>();

        seekers.addAll(game.getPlayers());

        hiders.add(game.getPlayers().toArray(new Player[0])[(int) (Math.random() * game.getPlayers().size())]);
        seekers.removeAll(hiders);

        timeSlot = InventoryCoordinateUtil.getCoordinateFromXY(0, getInventory().getSize() / 9 - 1);
        readySlot = InventoryCoordinateUtil.getCoordinateFromXY(8, getInventory().getSize() / 9 - 1);

        timeLeft = secondsToWait;
        timer = new BukkitRunnable() {
            @Override
            public void run() {
                if(--timeLeft == 0)
                    stop();
                updateInventory();
            }
        }.runTaskTimer(NoxetServer.getPlugin(), 60, 20);

        this.callback = callback;
    }

    @Override
    protected void updateInventory() {
        disbandRemovedPlayers();

        setSlotItem(ItemGenerator.generateItem(
                Material.STONE_AXE,
                "§c§lSEEKERS"
        ), 0, 0);

        setSlotItem(ItemGenerator.generateItem(
                Material.GOLDEN_CHESTPLATE,
                "§a§lHIDERS"
        ), 0, 1);

        int x = 0;
        for(Player seeker : seekers)
            setSlotItem(ItemGenerator.generatePlayerSkull(
                    seeker,
                    "§c§lSEEKER: §b" + seeker.getName(),
                    null
            ), ++x, 0);

        x = 0;

        for(Player hider : hiders)
            setSlotItem(ItemGenerator.generatePlayerSkull(
                    hider,
                    "§a§lHIDER: §b" + hider.getName(),
                    null
            ), ++x, 0);

        setSlotItem(ItemGenerator.generateItem(
                Material.CLOCK,
                timeLeft,
                "§c" + timeLeft + "§es",
                Arrays.asList("§euntil start.", "§aReady up to skip waiting.")
        ), timeSlot);

        List<String> readyUpLore = new ArrayList<>();
        readyUpLore.add("§eWaiting for:");
        Set<Player> notReadyPlayers = new HashSet<>();
        notReadyPlayers.addAll(seekers);
        notReadyPlayers.addAll(hiders);
        notReadyPlayers.removeAll(readyPlayers);

        for(Player notReadyPlayer : notReadyPlayers)
            readyUpLore.add("§7 - §c" + notReadyPlayer.getName());

        readyUpLore.add("§dSee yourself in the list? Click if you're ready.");

        setSlotItem(ItemGenerator.generateItem(
                Material.GREEN_CONCRETE,
                notReadyPlayers.size(),
                "§e" + readyPlayers.size() + "/" + (seekers.size() + hiders.size()) + " players ready.",
                readyUpLore
        ), readySlot);
    }

    @Override
    protected boolean onSlotClick(Player player, InventoryCoordinate coordinate, ClickType clickType) {
        disbandRemovedPlayers();

        if(coordinate.isAt(readySlot)) {
            readyPlayers.add(player);

            if(readyPlayers.size() == seekers.size() + hiders.size())
                return true; // Stop game if all are ready.
        }

        if(coordinate.getY() == 0) {
            // Seekers team

            hiders.remove(player);
            readyPlayers.remove(player);

            seekers.add(player);
        } else if(coordinate.getY() == 1) {
            // Hiders team

            seekers.remove(player);
            readyPlayers.remove(player);

            hiders.add(player);
        } else return false;

        updateInventory();

        return false;
    }

    @Override
    protected void stop() {
        disbandRemovedPlayers();

        timer.cancel();
        callback.accept(this);
        super.stop();
    }

    public Set<Player> getSeekers() {
        return seekers;
    }

    public Set<Player> getHiders() {
        return hiders;
    }

    public void disbandRemovedPlayers() {
        seekers.removeIf(seeker -> !game.isPlayer(seeker));
        hiders.removeIf(hider -> !game.isPlayer(hider));
        readyPlayers.removeIf(readyPlayer -> !game.isPlayer(readyPlayer));
    }
}
