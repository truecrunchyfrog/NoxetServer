package org.noxet.noxetserver.commands.misc;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.noxet.noxetserver.NoxetServer;
import org.noxet.noxetserver.messaging.NoxetErrorMessage;
import org.noxet.noxetserver.messaging.NoxetMessage;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class ChickenLeg implements CommandExecutor {
    private static final Set<Player> chickenLegPlayers = new HashSet<>();

    @Override
    @SuppressWarnings("NullableProblems")
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(!(commandSender instanceof Player)) {
            new NoxetErrorMessage(NoxetErrorMessage.ErrorType.COMMON, "chicken leg only for player!!!").send(commandSender);
            return true;
        }

        Player player = (Player) commandSender;

        if(chickenLegPlayers.add(player)) {
            new NoxetMessage("now chicken leg!!!!").send(player);
        } else
            stopChickenLeg(player);

        return true;
    }

    public static boolean isPlayerChickenLeg(Player player) {
        return chickenLegPlayers.contains(player);
    }

    public static void stopChickenLeg(Player player) {
        chickenLegPlayers.remove(player);
        new NoxetMessage("no longer chicken leg :(").send(player);
    }

    public static void summonChickenLeg(Player player) {
        Entity bomber = player.getWorld().spawnEntity(player.getLocation(), EntityType.values()[new Random().nextInt(EntityType.values().length)]);

        bomber.setVelocity(player.getLocation().getDirection().multiply(6));

        new BukkitRunnable() {
            @Override
            public void run() {
                if(!bomber.isDead())
                    bomber.getWorld().createExplosion(bomber.getLocation(), 3, true);
                bomber.remove();
            }
        }.runTaskLater(NoxetServer.getPlugin(), 60);
    }
}
