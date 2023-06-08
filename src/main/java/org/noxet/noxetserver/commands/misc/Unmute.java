package org.noxet.noxetserver.commands.misc;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.noxet.noxetserver.NoxetServer;
import org.noxet.noxetserver.messaging.NoxetErrorMessage;
import org.noxet.noxetserver.messaging.NoxetSuccessMessage;
import org.noxet.noxetserver.playerdata.PlayerDataManager;

import java.util.Collections;
import java.util.List;

public class Unmute implements TabExecutor {
    @Override
    @SuppressWarnings("NullableProblems")
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(!commandSender.isOp()) {
            new NoxetErrorMessage(NoxetErrorMessage.ErrorType.COMMON, "You must be an operator to unmute players.").send(commandSender);
            return true;
        }

        if(strings.length == 0) {
            new NoxetErrorMessage(NoxetErrorMessage.ErrorType.ARGUMENT, "Missing argument: player to unmute.").send(commandSender);
            return false;
        }

        Player playerToUnmute = NoxetServer.getPlugin().getServer().getPlayer(strings[0]);

        if(playerToUnmute == null) {
            new NoxetErrorMessage(NoxetErrorMessage.ErrorType.ARGUMENT, "That is not an online player.").send(commandSender);
            return true;
        }

        PlayerDataManager playerDataManager = new PlayerDataManager(playerToUnmute);

        if(!(boolean) playerDataManager.get(PlayerDataManager.Attribute.MUTED)) {
            new NoxetErrorMessage(NoxetErrorMessage.ErrorType.COMMON, "That player is not muted.").send(commandSender);
            return true;
        }

        playerDataManager.set(PlayerDataManager.Attribute.MUTED, false).save();
        new NoxetSuccessMessage(
                playerToUnmute.getName() + " has been unmuted and can now chat again.")
                .addButton("Mute", ChatColor.RED, "Redo the mute", "mute " + playerToUnmute.getName()).send(commandSender);

        return true;
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        if(strings.length == 1) {
            Player playerToRecommend = NoxetServer.getPlugin().getServer().getPlayer(strings[0]);
            if(playerToRecommend != null)
                return Collections.singletonList(playerToRecommend.getName());
        }

        return null;
    }
}
