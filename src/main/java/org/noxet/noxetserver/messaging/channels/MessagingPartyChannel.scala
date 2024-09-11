package org.noxet.noxetserver.messaging.channels

import org.bukkit.command.CommandSender
import org.noxet.noxetserver.minigames.party.Party

import scala.collection.Set
import scala.collection.immutable.HashSet

class MessagingPartyChannel(party: Party) extends MessagingChannel:
    override def getRecipients: Set[CommandSender] = party.getMembers