package org.noxet.noxetserver.minigames

import org.bukkit.GameMode

trait MiniGameOptions:
    def getId: String
    def getDisplayName: String

    def getMinPlayers: Int
    def getMaxPlayers: Int

    /**
     * @return `true` if the game allows players to join when the game has started ("mid-game"), otherwise `false`.
     */
    def allowPlayerDropIns: Boolean

    def getDefaultGameMode: GameMode

    def getSpectatorContract: MiniGameOptions.SpectatorContract

    def getWorldChunksSquared: Int

    def initGame: MiniGameController

    def shouldAnnounceAdvancements: Boolean

object MiniGameOptions:
    enum SpectatorContract:
        case ALL, ONLY_DEAD_PLAYERS