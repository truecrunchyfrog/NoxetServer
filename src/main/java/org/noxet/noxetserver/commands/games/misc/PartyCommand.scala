package org.noxet.noxetserver.commands.games.misc

import org.bukkit.command.{Command, CommandSender, TabExecutor}
import org.bukkit.entity.Player
import org.noxet.noxetserver.NoxetServer
import org.noxet.noxetserver.commands.RegisteredCommand
import org.noxet.noxetserver.menus.inventory.PartyMenu
import org.noxet.noxetserver.messaging.ErrorMessage.ErrorType.*
import org.noxet.noxetserver.messaging.{ErrorMessage, Message, NoteMessage, SuccessMessage}
import org.noxet.noxetserver.minigames.party.Party
import org.noxet.noxetserver.playerdata.PlayerDataManager

object PartyCommand extends PlayerTabExecutor, RegisteredCommand("party", this):
  override def onPlayerCommand(player: Player, command: Command, s: String, strings: Array[String]): Boolean =
    strings match
      case Array("create") if Party.isPlayerMemberOfParty(player) =>
        ErrorMessage(Common, "You are already in a party. Please leave the party first.").send(player)
        true
      case Array("create") =>
        Party(player)
        true
      case Array("invite") =>
        ErrorMessage(Argument, "Missing argument: player to invite.").send(player)
        true
      case Array("invite", playerName) =>
        Option(NoxetServer.getPlugin.getServer.getPlayer(playerName)) match
          case Some(playerToInvite) =>
            val party = Party.getPartyFromMember(player) match
              case Some(party) if !party.isOwner(player) =>
                ErrorMessage(Permission, "You are not the owner of this party, so you cannot invite players.").send(player)
                return true
              case Some(party) => party
              case None =>
                NoteMessage("You are not in a party. To save you time, we will create one for you.").send(player)
                Party(player)

            party.invitePlayer(playerToInvite)
            true
          case None =>
            ErrorMessage(Common, "Invalid player. You can only invite someone who is actually online.").send(player)
            true
      case Array("chat", args*) =>
        Party.getPartyFromMember(player) match
          case Some(_) if args.isEmpty =>
            ErrorMessage(Common, "Missing message.").send(player)
          case Some(party) =>
            party.sendChatMessage(player, args.mkString(" "))
          case None =>
            ErrorMessage(Common, "You are not in a party.").send(player)
        true
      case Array("accept") =>
        ErrorMessage(Argument, "Missing argument: invitation to accept.").send(player)
        true
      case Array("accept", invitedBy) =>
        Option(NoxetServer.getPlugin.getServer.getPlayer(invitedBy)) match
          case Some(partyOwner) =>
            Party.getOwnedParty(partyOwner) match
              case Some(party) if party.isPlayerInvited(player) =>
                party.acceptInvite(player)
              case _ =>
                ErrorMessage(Common,
                  s"There is no active invitation for you to a party owned by ${partyOwner.getName}. " +
                    "If you did receive an invitation, the party may have disbanded, " +
                    "the invitation withdrawn, or the ownership transferred.").send(player)
          case None =>
            ErrorMessage(ErrorMessage.ErrorType.COMMON, "Invalid player.").send(player)
        true
      case Array("deny") =>
        ErrorMessage(Argument, "Missing argument: invitation to deny.").send(player)
        true
      case Array("deny", invitedBy) =>
        Option(NoxetServer.getPlugin.getServer.getPlayer(invitedBy)) match
          case Some(partyOwner) =>
            Party.getOwnedParty(partyOwner) match
              case Some(party) if party.isPlayerInvited(player) =>
                party.denyInvite(player)
              case _ =>
                ErrorMessage(Common,
                  s"You do not have a pending invitation to a party owned by ${partyOwner.getName}.").send(player)
          case None =>
            ErrorMessage(Common, "Invalid player.").send(player)
        true
      case Array("kick") =>
        ErrorMessage(Argument, "Missing argument: player to kick.").send(player)
        true
      case Array("kick", playerName) =>
        Party.getPartyFromMember(player) match
          case Some(party) if !party.isOwner(player) =>
            ErrorMessage(Permission, "You are not the owner of this party, so you cannot kick players.").send(player)
          case Some(party) =>
            Option(NoxetServer.getPlugin.getServer.getPlayer(playerName)) match
              case Some(playerToKick) if !party.getMembers.contains(playerToKick) =>
                ErrorMessage(Common, "This player is not a member of your party.").send(player)
              case Some(playerToKick) if party.isOwner(playerToKick) =>
                ErrorMessage(Common, "Owners must disband the party to leave it.").send(player)
              case Some(playerToKick) =>
                party.kickMember(playerToKick)
              case None =>
                ErrorMessage(Common, "Invalid player.").send(player)
          case None =>
            ErrorMessage(Common, "You are not in a party.").send(player)
        true
      case Array("transfer") =>
        ErrorMessage(Argument, "Missing argument: player to transfer the party to.").send(player)
        true
      case Array("transfer", playerName) =>
        Party.getPartyFromMember(player) match
          case Some(party) if !party.isOwner(player) =>
            ErrorMessage(Permission, "You are not the owner of this party, so you cannot transfer it.").send(player)
          case Some(party) =>
            NoxetServer.getPlugin.getServer.getPlayer(playerName) match
              case Some(newOwner) if !party.getMembers.contains(newOwner) =>
                ErrorMessage(Common, "This player is not a member of your party.").send(player)
              case Some(newOwner) =>
                party.transfer(newOwner)
              case None =>
                ErrorMessage(Common, "Invalid player.").send(player)
          case None =>
            ErrorMessage(Common, "You are not in a party.").send(player)
        true
      case Array("leave") =>
        Party.getPartyFromMember(player) match
          case Some(party) if party.isOwner(player) =>
            ErrorMessage(Common, "You are the owner of this party. To leave it, you must disband it or transfer it.").addButton(
              "Disband whole party",
              ChatColor.RED,
              "Disband your party",
              "party disband"
            ).send(player)
          case Some(party) =>
            party.memberLeave(player)
          case None =>
            ErrorMessage(Common, "You are not in a party.").send(player)
        true
      case Array("list") =>
        Party.getPartyFromMember(player) match
          case Some(party) =>
            Message(s"§eMembers (with owner): ${party.getMembers.size}").send(commandSender)

            for member <- party.getMembers do
              val message = Message(s"└§6§lMEMBER §e${member.getName}")

              if party.isOwner(player) then
                message.addButton(
                  "Kick",
                  ChatColor.RED,
                  "Kick this player from the party",
                  s"party kick ${member.getName}"
                ).addButton(
                  "Transfer",
                  ChatColor.BLUE,
                  "Transfer the party ownership to this player",
                  s"party transfer ${member.getName}"
                )

              message.send(player)
          case None =>
            ErrorMessage(Common, "You are not in a party.").send(player)
        true
      case Array("disband") =>
        Party.getPartyFromMember(player) match
          case Some(party) if !party.isOwner(player) =>
            ErrorMessage(Permission, "You are not the owner of this party, so you cannot disband it.").send(player)
          case Some(party) =>
            party.disband()
          case None =>
            ErrorMessage(Common, "You are not in a party.").send(player)
        true
      case Array("kick-busy") =>
        Party.getPartyFromMember(player) match
          case Some(party) if !party.isOwner(player) =>
            ErrorMessage(Permission, "You are not the owner of " +
              "this party, so you cannot kick members from it.")
              .send(player)
          case Some(party) if !party.isPartyReadyForGame =>
            Message("§aThere are no busy players to kick!").send(player)
          case Some(party) =>
            SuccessMessage(s"Kicking in-game players: ${party.getBusyMembers.size}").send(player)
            party.kickBusyPlayers()
          case None =>
            ErrorMessage(Common, "You are not in a party.").send(player)
        true
      case Array("toggle-invites") =>
        val playerDataManager = PlayerDataManager(player)
        playerDataManager.toggleBoolean(PlayerDataManager.Attribute.DisallowIncomingPartyInvites)

        val newSetting = playerDataManager.get(PlayerDataManager.Attribute.DisallowIncomingPartyInvites)
          .asInstanceOf[Boolean]
        SuccessMessage(s"Party invites ${if newSetting then "enabled" else "disabled"}. " +
          s"You can ${if newSetting then "now" else "no longer"} receive party invites.").send(player)

        true
      case Array(subCommand, _*) =>
        ErrorMessage(Argument, s"Invalid subcommand '$subCommand'.").send(player)
        false
      case Array() =>
        PartyMenu(player).openInventory(player)
        true

  override def onTabComplete(player: Player, command: Command, s: String, strings: Array[String]): List[String] =
    strings match
      case Array(_) =>
        List("create", "invite", "chat", "accept", "deny",
          "kick", "transfer", "leave", "list", "disband",
          "kick-busy", "toggle-invites")
      case Array(subCommand, playerName) =>
        subCommand match
          case "invite" | "accept" | "deny" =>
            Option(NoxetServer.getPlugin.getServer.getPlayer(playerName))
              .map(_.getName)
              .toList
          case "kick" | "transfer" =>
            Party.getOwnedParty(player)
              .toList
              .flatMap(p => p.getMembers.filterNot(p.isOwner))