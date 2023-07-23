package org.noxet.noxetserver.util;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Objects;

public class EntityGlowColor {
    /**
     * Set the glow color of an entity (this will NOT make the entity glow, use Entity#setGlowing).
     * @param entity The entity to set the glow color for
     */
    public static void setGlowColor(Entity entity, ChatColor color) {
        Scoreboard scoreboard = Objects.requireNonNull(Bukkit.getScoreboardManager()).getNewScoreboard();
        Team team = scoreboard.registerNewTeam("color");

        team.setColor(color);
        team.addEntry(entity.getName());
    }
}
