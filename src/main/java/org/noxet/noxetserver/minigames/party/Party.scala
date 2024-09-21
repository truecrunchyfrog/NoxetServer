package org.noxet.noxetserver.minigames.party

import org.bukkit.entity.Player
import org.noxet.noxetserver.messaging.channels.MessagingPartyChannel
import org.noxet.noxetserver.messaging.{ErrorMessage, Message, MessagingContext}
import org.noxet.noxetserver.minigames.MiniGameManager
import org.noxet.noxetserver.playerdata.PlayerDataManager
import org.noxet.noxetserver.util.TextBeautifier

import scala.collection.mutable

class Party(private var _owner: Player):

  import Party.*

  private val members: mutable.Set[Player] = mutable.HashSet()
  private val invitedPlayers: mutable.Set[Player] = mutable.HashSet()
  private val messagingContext =
    MessagingContext(s"§e§l${TextBeautifier.beautify("Party")} ", MessagingPartyChannel(this))

  addMember(owner)
  sendPartyMessage(Message("§eParty created! Use §d/party invite §5<player>§e to invite a player to the party."))

  private def addMember(newMember: Player): Unit =
    if !isOwner(newMember) then
      Message("§aYou were added to the party.").send(newMember)
      sendPartyMessage(Message(s"§a${newMember.getName} is now a party member."))

    members.add(newMember)
    partyMembers.put(newMember, this)

  private def removeMember(member: Player): Unit =
    if !members.contains(member) then return

    if isOwner(member) then
      disband()
      return

    members.remove(member)
    partyMembers.remove(member)

  /**
   * When a member leaves the party by themselves.
   *
   * @param member The player who wants to leave the party
   */
  def memberLeave(member: Player): Unit =
    removeMember(member)

    Message(s"§aYou left ${owner.getName}'s party.").send(member)
    sendPartyMessage(Message(s"§c${member.getName} left the party."))

  def kickMember(member: Player): Unit =
    removeMember(member)
    sendPartyMessage(Message(s"§c${member.getName} was kicked from the party."))
    Message(s"§cYou were kicked from ${owner.getName}'s party.").send(member)

  def invitePlayer(player: Player): Unit =
    if members.contains(player) then
      ErrorMessage(ErrorMessage.ErrorType.Common, "This player is already in the party.").send(owner)
      return

    val playerDataManager = PlayerDataManager(player)

    if playerDataManager.doesContain(PlayerDataManager.Attribute.BlockedPlayers, owner.getUniqueId.toString) ||
      playerDataManager.get(PlayerDataManager.Attribute.DisallowIncomingPartyInvites).asInstanceOf[Boolean]
    then
      ErrorMessage(ErrorMessage.ErrorType.Common, "You may not invite this player.").send(owner)
      return

    if invitedPlayers.add(player) then
      Message(s"§eYou have been invited to $owner's party.\n")
        .addButton(
          "Accept",
          ChatColor.GREEN,
          "Join this party",
          s"party accept ${owner.getName}"
        )
        .addButton(
          "Deny",
          ChatColor.RED,
          "Deny this invitation",
          s"party deny ${owner.getName}"
        ).send(player)
      Message(s"§eInvited ${player.getName} to the party.").send(owner)
    else
      ErrorMessage(ErrorMessage.ErrorType.Common, "This player has already been invited to the party.").send(owner)

  def acceptInvite(player: Player): Unit =
    if isPlayerMemberOfParty(player) then
      ErrorMessage(ErrorMessage.ErrorType.Common, "You are already in another party.").addButton(
        "Leave current party",
        ChatColor.RED,
        "Leave the party you are in now, to be able to join another party",
        "party leave"
      ).send(player)
      return

    if members.size >= 50 then
      ErrorMessage(ErrorMessage.ErrorType.Common, "Sorry, there are too many members in this party!").send(player)
      sendPartyMessage(Message(s"§c${player.getName} cannot join the game, because there are too many members."))
      return

    if !invitedPlayers.remove(player) then
      return

    addMember(player)

  def denyInvite(player: Player): Unit =
    if !invitedPlayers.remove(player) then return

    Message(s"§aYou declined the invite from ${owner.getName}.").send(player)
    Message(s"§c${player.getName} declined your invite.").send(owner)

  def isPlayerInvited(player: Player): Boolean = invitedPlayers.contains(player)

  /**
   * Gets the members of this party. The owner is also a member!
   *
   * @return A list of the members
   */
  def getMembers: Set[Player] = members.toSet

  def owner: Player = owner

  def owner_=(newOwner: Player): Unit =
    if !members.contains(newOwner) then
      ErrorMessage(ErrorMessage.ErrorType.Common, "You cannot transfer party ownership to players outside the party.").send(owner)
      return

    if isOwner(newOwner) then
      ErrorMessage(ErrorMessage.ErrorType.Common, "You are already the owner!").send(owner)
      return

    sendPartyMessage(Message(s"§eThe party's leadership has been switched to ${newOwner.getName} (from ${owner.getName}). Take the wheel, ${newOwner.getName}!"))
    Message("§aYou are now the owner of this party.").send(newOwner)

    _owner = newOwner

  def isOwner(member: Player): Boolean = member == owner

  def sendPartyMessage(message: Message): Unit = messagingContext.broadcast(message)

  /**
   * Disband the party.
   */
  def disband(): Unit =
    sendPartyMessage(Message("§cThe party has disbanded."))
    partyMembers.keySet.diff(members)

  /**
   * Retrieve the members of the party that are busy inside games. The party owner is not included.
   *
   * @return a list of busy members.
   */
  def getBusyMembers: List[Player] =
    members.toList.filter(m => !isOwner(m) && MiniGameManager.isPlayerBusyInGame(m))

  def kickBusyPlayers(): Unit = getBusyMembers.foreach(kickMember)

  /**
   * Check whether the party can summon all members to a game (checks if players are busy already, to prevent).
   *
   * @return Whether a game can be joined
   */
  def isPartyReadyForGame: Boolean = getBusyMembers.isEmpty

  def sendChatMessage(member: Player, message: String): Unit =
    sendPartyMessage(Message(
      // Owner prefix:
      (if isOwner(member) then s"§9§l${TextBeautifier.beautify("owner")} " else "") +
        s"§f${member.getDisplayName}§9→ §b" + message
    ))


object Party:
  private val partyMembers: mutable.Map[Player, Party] = mutable.HashMap()

  def getPartyFromMember(member: Player): Option[Party] = partyMembers.get(member)

  def isPlayerMemberOfParty(player: Player): Boolean = partyMembers.contains(player)

  def getOwnedParty(owner: Player): Option[Party] = getPartyFromMember(owner) match
    case Some(party) if party.isOwner(owner) => Some(party)
    case _ => None

  def abandonPlayer(player: Player): Unit =
    getPartyFromMember(player).foreach(_.removeMember(player))