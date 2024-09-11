package org.noxet.noxetserver.messaging.channels

import org.bukkit.World
import org.bukkit.command.CommandSender

import scala.collection.Set

class MessagingWorldChannel(world: World) extends MessagingChannel:
    override def getRecipients: Set[CommandSender] = world.getPlayers