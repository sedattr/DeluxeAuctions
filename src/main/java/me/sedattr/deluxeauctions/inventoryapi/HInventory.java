package me.sedattr.deluxeauctions.inventoryapi;

import lombok.Getter;
import java.util.HashMap;

import me.sedattr.deluxeauctions.inventoryapi.inventory.InventoryVariables;
import me.sedattr.deluxeauctions.inventoryapi.item.ClickableItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class HInventory implements InventoryHolder {
    private final Inventory bukkitInventory;
    private final HashMap<Integer, ClickableItem> clickableItems;
    @Getter private final String id;
    @Getter private boolean closeable;

    public HInventory(String title, InventoryType inventoryType, int size, String id, boolean closeable) {
        this.clickableItems = new HashMap<>();
        this.id = id;
        this.closeable = closeable;

        if (inventoryType.equals(InventoryType.CHEST))
            this.bukkitInventory = Bukkit.createInventory(this, size * 9, title);
        else
            this.bukkitInventory = Bukkit.createInventory(this, inventoryType, title);
    }

    public void open(final Player player) {
        if (player == null)
            return;

        player.openInventory(this.bukkitInventory);
        InventoryVariables.addPlayerInventory(player, this);
    }

    public void close(Player player) {
        if (player == null)
            return;

        this.closeable = true;
        player.closeInventory();
    }

    public @NotNull Inventory getInventory() {
        return this.bukkitInventory;
    }

    public void setItem(int slot, ClickableItem clickableItem) {
        if (slot < 0 || slot >= this.bukkitInventory.getSize())
            return;

        if (clickableItem != null && clickableItem.getItem() != null && !clickableItem.getItem().getType().equals(Material.AIR)) {
            this.clickableItems.put(slot, clickableItem);
            this.bukkitInventory.setItem(slot, clickableItem.getItem());
        } else {
            this.clickableItems.remove(slot);
            this.bukkitInventory.setItem(slot, new ItemStack(Material.AIR));
        }
    }

    public ClickableItem getItem(int slot) {
        return this.clickableItems.getOrDefault(slot, null);
    }
}