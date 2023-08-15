package org.noxet.noxetserver.menus.inventory;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Consumer;
import org.noxet.noxetserver.NoxetServer;
import org.noxet.noxetserver.menus.ItemGenerator;
import org.noxet.noxetserver.messaging.ErrorMessage;
import org.noxet.noxetserver.minigames.MiniGameController;
import org.noxet.noxetserver.util.InventoryCoordinate;
import org.noxet.noxetserver.util.InventoryCoordinateUtil;
import org.noxet.noxetserver.util.Team;

import java.util.*;

public class TeamPickerMenu extends InventoryMenu {
    private final MiniGameController game;
    private final Map<Team, Set<Player>> playerTeams = new HashMap<>();
    private final Set<Player> readyPlayers = new HashSet<>();
    private final BukkitTask timer;
    private int timeLeft;
    private final Consumer<TeamPickerMenu> callback;
    private final InventoryCoordinate timeSlot, readySlot;

    public TeamPickerMenu(MiniGameController game, List<Team> teams, int secondsToWait, Consumer<TeamPickerMenu> callback) {
        super(teams.size() + 1, "Choose a Team", true);

        this.game = game;

        timeSlot = InventoryCoordinateUtil.getCoordinateFromXY(0, getInventory().getSize() / 9 - 1);
        readySlot = InventoryCoordinateUtil.getCoordinateFromXY(8, getInventory().getSize() / 9 - 1);

        int maxPlayers = 0;

        for(Team team : teams)
            maxPlayers += team.getMaxTeamMembers();

        if(maxPlayers < game.getPlayers().size())
            throw new IllegalStateException("Cannot create team picker menu for a game with more players than the teams together can fit!");

        Iterator<Player> playerIterator = game.getPlayers().iterator();
        Iterator<Team> teamIterator = teams.iterator();

        while(playerIterator.hasNext()) {
            /*
            Instead of filling each team one at a time like:

            TEAM 1: X X X X X X
            TEAM 2: X
            TEAM 3: X

            We try each one from first to last with each player to fill as fairly as possible:

            TEAM 1: X X X
            TEAM 2: X X X
            TEAM 3: X X

            We ascend and leave one player and move on (and skipping full teams).
             */

            while(teamIterator.hasNext()) {
                Team team = teamIterator.next();
                Set<Player> teamPlayers = playerTeams.getOrDefault(team, new HashSet<>());

                if(team.getMaxTeamMembers() > teamPlayers.size()) {
                    teamPlayers.add(playerIterator.next()); // Team is not full, let them in!
                    break;
                }
            }

            if(!teamIterator.hasNext())
                teamIterator = teams.iterator();
        }

        timeLeft = secondsToWait;
        timer = new BukkitRunnable() {
            @Override
            public void run() {
                if(--timeLeft == 0 || game.hasEnded())
                    stop();
                updateInventory();
            }
        }.runTaskTimer(NoxetServer.getPlugin(), 120, 20);

        this.callback = callback;
    }

    @Override
    protected void updateInventory() {
        disbandRemovedPlayers();

        int y = 0;

        for(Map.Entry<Team, Set<Player>> teamSetEntry : playerTeams.entrySet()) {
            Team team = teamSetEntry.getKey();
            Set<Player> teamPlayers = teamSetEntry.getValue();

            setSlotItem(ItemGenerator.generateItem(
                    team.getTeamIcon(),
                    "§7Team",
                    Collections.singletonList(team.getDisplayName().toUpperCase() + 'S')
            ), 0, y);

            int x = 1;

            Iterator<Player> playerIterator = teamPlayers.iterator();

            int skip = teamPlayers.size() > 8 ? timeLeft % (teamPlayers.size() - 8) : 0;

            while(playerIterator.hasNext()) {
                Player thisPlayer = playerIterator.next();

                if(skip > 0) {
                    skip--;
                    continue;
                }

                setSlotItem(ItemGenerator.generatePlayerSkull(
                        thisPlayer,
                         team.getDisplayName()+ ": §b" + thisPlayer.getName(),
                        null
                ), x++, y);
            }

            while(++x < 9)
                setSlotItem(ItemGenerator.generateItem(
                        Material.LIGHT_GRAY_STAINED_GLASS_PANE,
                        team.getMaxTeamMembers() > teamPlayers.size() ? "§7Click to play as a" : "§c" + (timeLeft % 3 == 0 ? "§n" : "") + "This team is full!",
                        Collections.singletonList(team.getDisplayName())
                ), x, y);

            y++;
        }

        setSlotItem(ItemGenerator.generateItem(
                timeLeft % 2 == 0 ? Material.CLOCK : Material.LIGHT_GRAY_STAINED_GLASS_PANE,
                timeLeft,
                "§c" + timeLeft + "§es",
                Arrays.asList("§euntil start.", "§aReady up to skip waiting.")
        ), timeSlot);

        List<String> readyUpLore = new ArrayList<>();
        readyUpLore.add("§eWaiting for:");

        Set<Player> notReadyPlayers = getPlayers();
        notReadyPlayers.removeAll(readyPlayers);

        notReadyPlayers.forEach(player -> readyUpLore.add("§7 - §c" + player.getName()));

        readyUpLore.add("§dSee yourself in the list? Click here to start early.");

        setSlotItem(ItemGenerator.generateItem(
                Material.GREEN_CONCRETE,
                notReadyPlayers.size(),
                "§e" + readyPlayers.size() + "/" + getPlayers().size() + " players ready.",
                readyUpLore
        ), readySlot);
    }

    @Override
    protected boolean onSlotClick(Player player, InventoryCoordinate coordinate, ClickType clickType) {
        disbandRemovedPlayers();

        if(coordinate.isAt(readySlot)) {
            readyPlayers.add(player);

            if(readyPlayers.size() == getPlayers().size())
                return true; // Start game - stop menu - if all are ready.
        }

        int y = 0;

        for(Map.Entry<Team, Set<Player>> teamSetEntry : playerTeams.entrySet()) {
            Team team = teamSetEntry.getKey();
            Set<Player> teamPlayers = teamSetEntry.getValue();

            if(coordinate.getY() == y++) {
                if(teamPlayers.size() + 1 > team.getMaxTeamMembers()) {
                    new ErrorMessage(ErrorMessage.ErrorType.COMMON, "This team is full!").send(player);
                    player.playSound(player, Sound.ENTITY_VILLAGER_NO, 1, 0.5f);
                    return false;
                }

                readyPlayers.remove(player);

                playerTeams.forEach((team1, players) -> {
                    if(team1 != team)
                        players.remove(player);
                });

                teamPlayers.add(player);

                updateInventory();

                break;
            }
        }

        return false;
    }

    @Override
    protected void stop() {
        disbandRemovedPlayers();

        timer.cancel();
        if(!game.hasEnded())
            callback.accept(this);

        super.stop();
    }

    public Map<Team, Set<Player>> getPlayerTeams() {
        return playerTeams;
    }

    private Set<Player> getPlayers() {
        Set<Player> allPlayers = new HashSet<>();
        playerTeams.forEach((team, players) -> allPlayers.addAll(players));
        return allPlayers;
    }

    public void disbandRemovedPlayers() {
        playerTeams.forEach((team, players) -> players.removeIf(player -> !game.isPlayer(player)));
        readyPlayers.removeIf(readyPlayer -> !game.isPlayer(readyPlayer));
    }
}
