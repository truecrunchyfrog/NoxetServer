package org.noxet.noxetserver.playerstate.properties;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.noxet.noxetserver.playerstate.PlayerStateProperty;

public class PSPGameMode implements PlayerStateProperty<String> {
    @Override
    public String getConfigName() {
        return "game_mode";
    }

    @Override
    public String getDefaultSerializedProperty() {
        return GameMode.SURVIVAL.name();
    }

    @Override
    public String getSerializedPropertyFromPlayer(Player player) {
        return player.getGameMode().name();
    }

    @Override
    public void restoreProperty(Player player, String mode) {
        player.setGameMode(GameMode.valueOf(mode));
    }

    @Override
    public Class<String> getTypeClass() {
        return String.class;
    }
}
