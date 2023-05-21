package org.noxet.noxetserver.commands.smp;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.noxet.noxetserver.NoxetServer;
import org.noxet.noxetserver.RealmManager;
import org.noxet.noxetserver.messaging.NoxetErrorMessage;
import org.noxet.noxetserver.messaging.NoxetMessage;

import java.util.Objects;
import java.util.Random;

import static org.noxet.noxetserver.RealmManager.getCurrentRealm;

public class Wild implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(!(commandSender instanceof Player)) {
            new NoxetErrorMessage("Only players can go to the wild.").send(commandSender);
            return true;
        }

        Player player = (Player) commandSender;
        RealmManager.Realm realm = getCurrentRealm(player);

        if(realm != RealmManager.Realm.SMP) {
            new NoxetErrorMessage("Wilderness teleportation is not supported here.").send(player);
            return true;
        }

        new NoxetMessage("Wilderness teleportation commencing...").send(player);

        Location teleportTo = new Location(
                NoxetServer.ServerWorld.SMP_WORLD.getWorld(),
                0,
                0,
                0
        );

        teleportTo.setX(getRandomXZValue());
        teleportTo.setZ(getRandomXZValue());

        teleportTo.setY(Objects.requireNonNull(teleportTo.getWorld()).getHighestBlockYAt(teleportTo));

        player.teleport(teleportTo);

        new NoxetMessage("Welcome to the wild!").send(player);

        return true;
    }

    int getRandomXZValue() {
        Random random = new Random();
        return random.nextInt((int) -Math.pow(10, 8), (int) Math.pow(10, 8));
    }
}
