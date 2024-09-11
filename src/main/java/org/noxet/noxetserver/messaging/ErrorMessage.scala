package org.noxet.noxetserver.messaging

import net.md_5.bungee.api.ChatColor

class ErrorMessage(kind: ErrorMessage.ErrorType, text: String) extends Message(text):
    override def getDefaultColor: ChatColor = ChatColor.RED
    override def getPrefix: String =
        super.getPrefix + "§4Error" + kind.desc.map(d => s" (§7$d§4)").getOrElse("") + ": "
        
object ErrorMessage:
    enum ErrorType(val desc: Option[String]):
        case Common extends ErrorType(None)
        case Permission extends ErrorType(Some("missing permission"))
        case Argument extends ErrorType(Some("incorrect argument usage"))