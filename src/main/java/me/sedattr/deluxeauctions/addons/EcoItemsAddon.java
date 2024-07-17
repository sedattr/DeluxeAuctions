package me.sedattr.deluxeauctions.addons;

import com.willfp.eco.core.items.Items;
import com.willfp.eco.core.items.TestableItem;
import org.bukkit.inventory.ItemStack;

public class EcoItemsAddon {
    public ItemStack getEcoItem(String name) {
        if (name == null || name.isEmpty())
            return null;

        TestableItem item = Items.lookup(name);
        return item.getItem();
    }
}
