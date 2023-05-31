package org.noxet.noxetserver.commands.misc;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.noxet.noxetserver.NoxetServer;
import org.noxet.noxetserver.messaging.NoxetErrorMessage;
import org.noxet.noxetserver.messaging.NoxetMessage;

import java.util.HashSet;
import java.util.Set;

public class ChickenLeg implements CommandExecutor {
    private static final Set<Player> chickenLegPlayers = new HashSet<>();

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(!(commandSender instanceof Player)) {
            new NoxetErrorMessage("chicken leg only for player!!!").send(commandSender);
            return true;
        }

        Player player = (Player) commandSender;

        if(chickenLegPlayers.add(player)) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    chickenLegPlayers.remove(player);
                    new NoxetMessage("no more chicken leg :(").send(player);
                }
            }.runTaskLater(NoxetServer.getPlugin(), 300);

            new NoxetMessage("now chicken leg!!!!").send(player);
        } else
            new NoxetErrorMessage("Already chicken leg!!!").send(player);

        return true;
    }

    public static boolean isPlayerChickenLeg(Player player) {
        return chickenLegPlayers.contains(player);
    }

    public static void summonChickenLeg(Player player) {
        Chicken chicken = (Chicken) player.getWorld().spawnEntity(player.getLocation(), EntityType.CHICKEN);

        chicken.setVelocity(player.getLocation().getDirection().multiply(3));

        new BukkitRunnable() {
            @Override
            public void run() {
                if(!chicken.isDead())
                    chicken.getWorld().createExplosion(chicken.getLocation(), 8, true);
            }
        }.runTaskLater(NoxetServer.getPlugin(), 60);
    }
}
