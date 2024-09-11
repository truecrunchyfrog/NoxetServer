package org.noxet.noxetserver.messaging;

import net.md_5.bungee.api.ChatColor;

class SuccessMessage(text: String) extends Message(text):
    override def getDefaultColor: ChatColor = ChatColor.GREEN
    override def getPrefix: String = super.getPrefix + "§2Success: "
    