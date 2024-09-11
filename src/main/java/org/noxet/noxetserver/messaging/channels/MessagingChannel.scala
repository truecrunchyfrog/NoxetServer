package org.noxet.noxetserver.messaging.channels

import org.bukkit.command.CommandSender

import scala.collection.Set

trait MessagingChannel:
    def getRecipients: Set[CommandSender]