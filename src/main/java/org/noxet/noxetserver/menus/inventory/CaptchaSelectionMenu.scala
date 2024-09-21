package org.noxet.noxetserver.menus.inventory

import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.noxet.noxetserver.util.{Captcha, InventoryCoordinate, InventoryCoordinateUtil, TextBeautifier}
import org.noxet.noxetserver.menus.ItemGenerator

class CaptchaSelectionMenu(question: Captcha.CaptchaQuestion)
  extends InventoryMenu(3,
    TextBeautifier.beautify(s"Sound ${question.soundIndex}/${question.captchaSounds.size} - What did you hear?", false),
    true):
  assert(question.captchaSounds.size == Captcha.answersPerQuestion)

  private val soundSlots: List[(Captcha.CaptchaSound, InventoryCoordinate)] =
    question.captchaSounds.zipWithIndex.map(x => (x._1, InventoryCoordinate(x._2 * 2 + 2, 1)))

  override protected def updateInventory(): Unit =
    soundSlots.foreach(s =>
      setSlotItem(
        ItemGenerator.generateItem(
          s._1.getMaterial,
          s"§d${s._1.getName}",
          List("§e→ Click if you heard this sound.")
        ), s._2))

  override protected def onSlotClick(player: Player, coordinate: InventoryCoordinate, clickType: ClickType): Boolean =
    val clickedSlot = soundSlots.find(_._2 == coordinate)
    clickedSlot.foreach(question.callback(_._1))
    clickedSlot.isDefined