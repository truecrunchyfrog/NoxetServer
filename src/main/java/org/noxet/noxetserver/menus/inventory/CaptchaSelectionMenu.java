package org.noxet.noxetserver.menus.inventory;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.util.Consumer;
import org.noxet.noxetserver.util.Captcha;
import org.noxet.noxetserver.menus.ItemGenerator;
import org.noxet.noxetserver.util.TextBeautifier;
import org.noxet.noxetserver.util.InventoryCoordinate;
import org.noxet.noxetserver.util.InventoryCoordinateUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CaptchaSelectionMenu extends InventoryMenu {
    private final List<Captcha.CaptchaSound> captchaSounds;
    private final List<InventoryCoordinate> soundSlots;
    private final Consumer<Captcha.CaptchaSound> callback;

    public CaptchaSelectionMenu(List<Captcha.CaptchaSound> captchaSounds, int soundIndex, int totalSounds, Consumer<Captcha.CaptchaSound> callback) {
        super(3, TextBeautifier.beautify("Sound " + soundIndex + "/" + totalSounds + " - What did you hear?", false), true);

        assert captchaSounds.size() == 3;
        this.captchaSounds = captchaSounds;

        soundSlots = new ArrayList<>();

        for(Captcha.CaptchaSound captchaSound : captchaSounds)
            soundSlots.add(InventoryCoordinateUtil.getCoordinateFromXY(captchaSounds.indexOf(captchaSound) * 2 + 2, 1));

        this.callback = callback;
    }

    @Override
    protected void updateInventory() {
        for(Captcha.CaptchaSound captchaSound : captchaSounds) {
            setSlotItem(
                    ItemGenerator.generateItem(
                            captchaSound.getMaterial(),
                            "§d" + captchaSound.getName(),
                            Collections.singletonList("§e→ Click if you heard this sound.")
                    ), soundSlots.get(captchaSounds.indexOf(captchaSound))
            );
        }
    }

    @Override
    protected boolean onSlotClick(Player player, InventoryCoordinate coordinate, ClickType clickType) {
        for(InventoryCoordinate soundSlot : soundSlots)
            if(coordinate.isAt(soundSlot)) {
                callback.accept(captchaSounds.get(soundSlots.indexOf(soundSlot)));
                return true;
            }

        return false;
    }
}
