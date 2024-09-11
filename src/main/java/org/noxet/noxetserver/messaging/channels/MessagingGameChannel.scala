package org.noxet.noxetserver.messaging.channels

import org.bukkit.command.CommandSender
import org.noxet.noxetserver.minigames.MiniGameController

import scala.collection.Set
import scala.jdk.CollectionConverters.*

class MessagingGameChannel(miniGameController: MiniGameController) extends MessagingChannel:
    override def getRecipients: Set[CommandSender] = miniGameController.getPlayersAndSpectators