package org.noxet.noxetserver.menus.book;

import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.List;

public class BookMenu {
    private final ItemStack book;

    public BookMenu(List<BaseComponent[]> pages) {
        book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta bookMeta = (BookMeta) book.getItemMeta();

        assert bookMeta != null;
        bookMeta.spigot().setPages(pages);

        bookMeta.setAuthor("NOXET");
        bookMeta.setTitle("Book Menu");

        book.setItemMeta(bookMeta);
    }

    public void openMenu(Player player) {
        player.openBook(book);
    }
}
