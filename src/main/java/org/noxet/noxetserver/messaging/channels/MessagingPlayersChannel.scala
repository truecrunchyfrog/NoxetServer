package org.noxet.noxetserver.messaging.channels

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

import scala.collection.Set

class MessagingPlayersChannel(players: Set[Player]) extends MessagingChannel:
    override def getRecipients: Set[CommandSender] = players