package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.noxet.noxetserver.NoxetServer;
import org.noxet.noxetserver.minigames.MiniGameManager;
import org.noxet.noxetserver.playerstate.PlayerStateProperty;

import java.util.Objects;

public class PSPScoreboard implements PlayerStateProperty<Scoreboard> {
    @Override
    public String getConfigName() {
        return "scoreboard";
    }

    @Override
    public Scoreboard getDefaultSerializedProperty() {
        return null;
    }

    @Override
    public Scoreboard getSerializedPropertyFromPlayer(Player player) {
        return null;
    }

    @Override
    public void restoreProperty(Player player, Scoreboard value) {
        // We only reset the scoreboard!
        if(!MiniGameManager.isPlayerBusyInGame(player)) // Don't interrupt game scoreboards.
            player.setScoreboard(Objects.requireNonNull(NoxetServer.getPlugin().getServer().getScoreboardManager()).getNewScoreboard());
    }

    @Override
    public Scoreboard getValueFromConfig(ConfigurationSection config) {
        return null;
    }
}
