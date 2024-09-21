package org.noxet.noxetserver.menus

import org.bukkit.{Material, OfflinePlayer}
import org.bukkit.inventory.{ItemFlag, ItemStack}
import org.bukkit.inventory.meta.SkullMeta

import scala.jdk.CollectionConverters.*

object ItemGenerator:
  def generateItem(material: Material, amount: Int = 1, name: String, lore: Option[List[String]] = None): ItemStack =
    val itemStack = ItemStack(material)

    val itemMeta = itemStack.getItemMeta
    assert(itemMeta != null)

    itemMeta.setDisplayName(name)
    lore.foreach(l => itemMeta.setLore(l.asJava))
    itemMeta.addItemFlags(
      ItemFlag.HIDE_POTION_EFFECTS,
      ItemFlag.HIDE_ATTRIBUTES,
      ItemFlag.HIDE_DESTROYS,
      ItemFlag.HIDE_DYE,
      ItemFlag.HIDE_ENCHANTS,
      ItemFlag.HIDE_UNBREAKABLE,
      ItemFlag.HIDE_PLACED_ON
    )

    itemStack.setItemMeta(itemMeta)
    itemStack.setAmount(amount)

    itemStack

  def generatePlayerSkull(skullPlayer: OfflinePlayer, name: String, lore: Option[List[String]]): ItemStack =
    val itemStack = ItemStack(Material.PLAYER_HEAD)

    val skullMeta = itemStack.getItemMeta.asInstanceOf[SkullMeta]
    assert(skullMeta != null)

    skullMeta.setOwningPlayer(skullPlayer)
    skullMeta.setDisplayName(name)
    lore.foreach(l => skullMeta.setLore(l.asJava))

    itemStack.setItemMeta(skullMeta)

    itemStack