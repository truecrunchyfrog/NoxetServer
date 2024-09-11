package org.noxet.noxetserver.messaging.channels

import org.bukkit.command.CommandSender
import org.noxet.noxetserver.realm.RealmManager.Realm

import scala.collection.Set

class MessagingRealmChannel(realm: Realm) extends MessagingChannel:
    override def getRecipients: Set[CommandSender] = realm.getPlayers