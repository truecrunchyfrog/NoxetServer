package org.noxet.noxetserver.messaging;

import net.md_5.bungee.api.ChatColor;

class WarningMessage(text: String) extends Message(text):
    override def getDefaultColor: ChatColor = ChatColor.YELLOW
    override def getPrefix: String = super.getPrefix + "§c§lWARNING: "