package org.noxet.noxetserver.menus.book

import net.md_5.bungee.api.chat.BaseComponent
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BookMeta

class BookMenu(pages: java.util.List[Array[BaseComponent]]):
  private val book = ItemStack(Material.WRITTEN_BOOK)
  private val meta = book.getItemMeta.asInstanceOf[BookMeta]
  meta.spigot.setPages(pages)
  meta.setAuthor("NOXET")
  meta.setTitle("Book Menu")

  book.setItemMeta(meta)

  def openMenu(player: Player): Unit = player.openBook(book)