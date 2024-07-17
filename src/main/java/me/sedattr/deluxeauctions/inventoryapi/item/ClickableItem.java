package me.sedattr.deluxeauctions.inventoryapi.item;

import lombok.Getter;
import org.bukkit.inventory.ItemStack;

@Getter
public class ClickableItem {
    private final ItemStack item;
    private final ClickInterface click;

    private ClickableItem(ItemStack item, ClickInterface click) {
        this.item = item;
        this.click = click;
    }

    public static ClickableItem of(ItemStack item, ClickInterface click) {
        return new ClickableItem(item, click);
    }

    public static ClickableItem empty(ItemStack item) {
        return new ClickableItem(item, null);
    }
}