package org.noxet.noxetserver.commands.misc;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.noxet.noxetserver.UsernameStorageManager;
import org.noxet.noxetserver.messaging.NoxetErrorMessage;
import org.noxet.noxetserver.messaging.NoxetMessage;
import org.noxet.noxetserver.playerdata.PlayerDataManager;

import java.util.List;
import java.util.UUID;

public class BlockList implements CommandExecutor {
    @Override
    @SuppressWarnings("NullableProblems")
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(!(commandSender instanceof Player)) {
            new NoxetErrorMessage(NoxetErrorMessage.ErrorType.COMMON, "Only players can list who they have blocked.").send(commandSender);
            return true;
        }

        Player player = (Player) commandSender;

        @SuppressWarnings("unchecked")
        List<String> blockedUUIDs = (List<String>) new PlayerDataManager(player).get(PlayerDataManager.Attribute.BLOCKED_PLAYERS);

        new NoxetMessage("§eBlocked players: " + blockedUUIDs.size()).send(player);

        for(String blockedUUIDString : blockedUUIDs) {
            UUID blockedUUID = new UsernameStorageManager().getUUIDFromUsernameOrUUID(blockedUUIDString);
            String blockedName = UsernameStorageManager.getCasedUsernameFromUUID(blockedUUID);
            new NoxetMessage("└§4§lBLOCKED §c" + (blockedName != null ? blockedName : blockedUUIDString))
                    .addButton("Pardon", ChatColor.GREEN, "Unblock this player", "unblock " + blockedUUIDString)
                    .send(player);
        }

        return true;
    }
}
