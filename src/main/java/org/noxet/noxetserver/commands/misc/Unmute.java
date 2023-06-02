package org.noxet.noxetserver.commands.misc;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.noxet.noxetserver.NoxetServer;
import org.noxet.noxetserver.messaging.NoxetErrorMessage;
import org.noxet.noxetserver.messaging.NoxetMessage;
import org.noxet.noxetserver.playerdata.PlayerDataManager;

import java.util.Collections;
import java.util.List;

public class Unmute implements TabExecutor {
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(!commandSender.isOp()) {
            new NoxetErrorMessage("You must be an operator to unmute players.").send(commandSender);
            return true;
        }

        if(strings.length == 0) {
            new NoxetErrorMessage("Missing argument: player to unmute.").send(commandSender);
            return false;
        }

        Player playerToUnmute = NoxetServer.getPlugin().getServer().getPlayer(strings[0]);

        if(playerToUnmute == null) {
            new NoxetErrorMessage("That is not a player.").send(commandSender);
            return true;
        }

        PlayerDataManager playerDataManager = new PlayerDataManager(playerToUnmute);

        if(!(boolean) playerDataManager.get(PlayerDataManager.Attribute.MUTED)) {
            new NoxetErrorMessage("That player is not muted.").send(commandSender);
            return true;
        }

        playerDataManager.set(PlayerDataManager.Attribute.MUTED, false).save();
        new NoxetMessage(
                playerToUnmute.getName() + " has been unmuted and can now chat again.")
                .addButton("Mute", ChatColor.RED, "Redo the mute", "mute " + playerToUnmute.getName()).send(commandSender);

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        if(strings.length == 1) {
            Player playerToRecommend = NoxetServer.getPlugin().getServer().getPlayer(strings[0]);
            if(playerToRecommend != null)
                return Collections.singletonList(playerToRecommend.getName());
        }

        return null;
    }
}
