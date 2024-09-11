package org.noxet.noxetserver.messaging;

import net.md_5.bungee.api.ChatColor;

class NoteMessage(text: String) extends Message(text):
    override def getDefaultColor: ChatColor = ChatColor.LIGHT_PURPLE
    override def getPrefix: String = super.getPrefix + "§5Note: "
    