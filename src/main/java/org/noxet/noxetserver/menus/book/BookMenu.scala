package org.noxet.noxetserver.menus.book

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BookMeta

class BookMenu(pages: java.util.List[Array[BaseComponent]]):
  val book = ItemStack(Material.WRITTEN_BOOK)
  book.setItemMeta({
    val meta = book.getItemMeta.asInstanceOf[BookMeta]
    meta.spigot.setPages(pages)
    meta.setAuthor("NOXET")
    meta.setTitle("Book Menu")
    meta
  })

  def openMenu(player: Player): Unit = player.openBook(book)