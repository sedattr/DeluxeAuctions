package me.sedattr.deluxeauctions.addons;

import com.willfp.ecoitems.items.EcoItem;
import com.willfp.ecoitems.items.EcoItems;
import org.bukkit.inventory.ItemStack;

public class EcoItemsAddon {
    public ItemStack getEcoItem(String name) {
        if (name == null || name.isEmpty())
            return null;

        EcoItem item = EcoItems.INSTANCE.getByID(name);
        if (item == null)
            return null;

        return item.getItemStack();
    }
}
