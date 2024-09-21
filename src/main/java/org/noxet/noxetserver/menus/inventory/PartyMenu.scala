package org.noxet.noxetserver.menus.inventory

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.noxet.noxetserver.menus.ItemGenerator
import org.noxet.noxetserver.menus.chat.ChatPromptMenu
import org.noxet.noxetserver.minigames.party.Party
import org.noxet.noxetserver.util.{InventoryCoordinate, InventoryCoordinateUtil, TextBeautifier}

class PartyMenu(val player: Player) extends InventoryMenu(
  if Party.isPlayerMemberOfParty(player) then
    (Party.getPartyFromMember(player).getMembers.size - 1) / 9 + 2
  else
    1,
  "Party", false):
  private val party = Party.getPartyFromMember(player)

  private val extrasY = getInventory.getSize / 9 - 1
  private val addPlayerSlot: InventoryCoordinate = (0, extrasY)
  private val leavePartySlot: InventoryCoordinate = (8, extrasY)

  override protected def updateInventory(): Unit =
    party match
      case Some(value) =>
        setSlotItem(
          ItemGenerator.generateItem(
            Material.BLUE_WOOL,
            "§9Create a Party!",
            List("§7Play with a chosen", "§7group of players.")
          ), 0, 0
        )
        return
      case None => ()

    party.getMembers.zipWithIndex.foreach(
      setSlotItem(
        ItemGenerator.generatePlayerSkull(
          member,
          s"§e${member.getName}",
          (if party.isOwner(member) then
            s"§9§l${TextBeautifier.beautify("owner")}"
          else
            s"§7§l${TextBeautifier.beautify("member")}")
            :+
            (if party.isOwner(player) && member != player then
              List(
                "§e→ Double-click to §c§nkick§e.",
                "§e→ Shift-right-click to §9§nmake owner§e."
              ) else Nil)
        ), i
      )
    )

    if party.isOwner(player) then
      setSlotItem(
        ItemGenerator.generateItem(
          Material.PAPER,
          "§aInvite Player",
          List("§7Invite a player to", "§7your party.")
        ), addPlayerSlot
      )

      setSlotItem(
        ItemGenerator.generateItem(
          Material.RED_WOOL,
          "§cDisband Party",
          List("§7Give up on this party.")
        ), leavePartySlot
      )
    else
      setSlotItem(
        ItemGenerator.generateItem(
          Material.RED_WOOL,
          "§cLeave Party",
          List(s"§7Leave ${party.owner.getName}'s party.")
        ), leavePartySlot
      )

  override protected def onSlotClick(player: Player, coordinate: InventoryCoordinate, clickType: ClickType): Boolean =
    if party.isEmpty then
      if coordinate == 0 then
        player.performCommand("party create")
        PartyMenu(player).openInventory(player)
        return true
      return false

    if !party.isOwner(player) then
      if coordinate == leavePartySlot then
        player.performCommand("party leave")
        return true
      return false

    if coordinate == addPlayerSlot then
      ChatPromptMenu("player to invite", player, promptResponse =>
        player.performCommand(s"party invite ${promptResponse.getMessage}")
        PartyMenu(player).openInventory(player))
      return true
    else if coordinate == leavePartySlot then
      ConfirmationMenu(
        "Disband party?",
        player.performCommand("party disband"),
        PartyMenu(player).openInventory(player)).openInventory(player)
      return true

    if coordinate.slotIndex > party.getMembers.size - 1 then
      return false

    party.getMembers.get(coordinate.slotIndex) match
      case Some(member) =>
        clickType match
          case ClickType.DOUBLE_CLICK =>
            player.performCommand(s"party kick ${member.getName}")
          case ClickType.SHIFT_RIGHT =>
            player.performCommand(s"party transfer ${member.getName}")
          case _ =>
            return false

        PartyMenu(player).openInventory(player)
        true
      case None => false