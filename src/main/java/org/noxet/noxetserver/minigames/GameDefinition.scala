package org.noxet.noxetserver.minigames

import org.bukkit.GameMode
import org.noxet.noxetserver.minigames.MiniGameOptions.SpectatorContract
import org.noxet.noxetserver.minigames.worldeater

enum GameDefinition(val options: MiniGameOptions):
  case WorldEater extends GameDefinition(new MiniGameOptions {
    override def getId: String = "world-eater"

    override def getDisplayName: String = "World Eater"

    override def getMinPlayers: Int = 2

    override def getMaxPlayers: Int = 8

    override def allowPlayerDropIns: Boolean = false

    override def getDefaultGameMode: GameMode = GameMode.SURVIVAL

    override def getSpectatorContract: SpectatorContract = SpectatorContract.ALL

    override def getWorldChunksSquared: Int = 5

    override def initGame: MiniGameController = worldeater.WorldEater()

    override def shouldAnnounceAdvancements: Boolean = true
  })

  def createGame: MiniGameController = options.initGame