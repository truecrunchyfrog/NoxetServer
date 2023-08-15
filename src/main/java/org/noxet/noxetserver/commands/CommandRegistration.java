package org.noxet.noxetserver.commands;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.noxet.noxetserver.NoxetServer;
import org.noxet.noxetserver.commands.admin.*;
import org.noxet.noxetserver.commands.admin.realm.ResetRealmSpawn;
import org.noxet.noxetserver.commands.admin.realm.SetRealmSpawn;
import org.noxet.noxetserver.commands.debug.ClearPlayerDataCache;
import org.noxet.noxetserver.commands.debug.FakeCombatLog;
import org.noxet.noxetserver.commands.debug.PreloadMiniGameWorld;
import org.noxet.noxetserver.commands.debug.WhereAmI;
import org.noxet.noxetserver.commands.games.creepersweeper.ClearCreeperSweeperStats;
import org.noxet.noxetserver.commands.games.creepersweeper.CreeperSweeper;
import org.noxet.noxetserver.commands.games.misc.Game;
import org.noxet.noxetserver.commands.games.misc.GameSelector;
import org.noxet.noxetserver.commands.games.misc.PartyCommand;
import org.noxet.noxetserver.commands.misc.CanvasWorld;
import org.noxet.noxetserver.commands.misc.ChickenLeg;
import org.noxet.noxetserver.commands.misc.EnderChest;
import org.noxet.noxetserver.commands.realms.anarchy.Anarchy;
import org.noxet.noxetserver.commands.realms.smp.SMP;
import org.noxet.noxetserver.commands.realms.smp.Wild;
import org.noxet.noxetserver.commands.social.*;
import org.noxet.noxetserver.commands.teleportation.Home;
import org.noxet.noxetserver.commands.teleportation.Hub;
import org.noxet.noxetserver.commands.teleportation.Spawn;
import org.noxet.noxetserver.commands.teleportation.TeleportAsk;

import java.util.*;

public class CommandRegistration {
    private static class CommandDefinition {
        private final String name;
        private final CommandExecutor commandExecutor;

        public CommandDefinition(String name, CommandExecutor commandExecutor) {
            this.name = name;
            this.commandExecutor = commandExecutor;
        }
    }

    private static final Set<CommandDefinition> commandDefinitions = new HashSet<>(Arrays.asList(
            new CommandDefinition("smp", new SMP()),
            new CommandDefinition("anarchy", new Anarchy()),
            new CommandDefinition("hub", new Hub()),
            new CommandDefinition("spawn", new Spawn()),
            new CommandDefinition("canvas-world", new CanvasWorld()),

            new CommandDefinition("wild", new Wild()),

            new CommandDefinition("tpa", new TeleportAsk()),

            new CommandDefinition("whereami", new WhereAmI()),

            new CommandDefinition("games", new GameSelector()),

            new CommandDefinition("home", new Home()),

            new CommandDefinition("chickenleg", new ChickenLeg()),

            new CommandDefinition("doas", new DoAs()),

            new CommandDefinition("mute", new Mute()),
            new CommandDefinition("unmute", new Unmute()),

            new CommandDefinition("msg", new MsgConversation()),

            new CommandDefinition("toggle-preserve", new TogglePreserve()),

            new CommandDefinition("loop", new Loop()),

            new CommandDefinition("set-realm-spawn", new SetRealmSpawn()),
            new CommandDefinition("reset-realm-spawn", new ResetRealmSpawn()),

            new CommandDefinition("friend", new Friend()),
            new CommandDefinition("block", new Block()),
            new CommandDefinition("unblock", new Unblock()),
            new CommandDefinition("block-list", new BlockList()),

            new CommandDefinition("clear-player-data-cache", new ClearPlayerDataCache()),

            new CommandDefinition("fake-combat-log", new FakeCombatLog()),

            new CommandDefinition("creeper-sweeper", new CreeperSweeper()),
            new CommandDefinition("clear-creeper-sweeper-stats", new ClearCreeperSweeperStats()),

            new CommandDefinition("enderchest", new EnderChest()),

            new CommandDefinition("game", new Game()),

            new CommandDefinition("party", new PartyCommand()),

            new CommandDefinition("preload-mini-game-world", new PreloadMiniGameWorld())
    ));

    // Remember to define command in src/main/resources/plugin.yml, too!
    
    public static void registerCommands() {
        for(CommandDefinition commandDefinition : commandDefinitions) {
            PluginCommand command = NoxetServer.getPlugin().getCommand(commandDefinition.name);
            
            if(command == null) {
                NoxetServer.logSevere("Command definition: '" + commandDefinition.name + "' for '" + commandDefinition.commandExecutor + "' not found!");
                continue;
            }
            
            command.setExecutor(commandDefinition.commandExecutor);
        }
    }
}
